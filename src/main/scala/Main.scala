package nnepso

import cilib._
import cilib.exec._
import cilib.io._
// import cilib.pso._
import zio.ZIOAppArgs
import zio.cli.HelpDoc.Span.text
import zio.cli._
import zio.stream._

import Util._
import Heuristics._

object main extends zio.ZIOAppDefault {

  // Global constants
  val threads = 8
  val swarmSize = 20
  // val n_iter = 100
  val problemDimensions = 5
  val bounds = Interval(-100.0, 100.0) ^ problemDimensions
  val n_runs = 5

  /*
   * CLI logic
   * for new algorithm:
   * - Cmd object,
   * - command value
   * - add as subcommand to `run` command
   * - add logic to CliApp.make(){}
   */
  final case class SimOptions(problem: String, iterations: Int)

  sealed trait Cmd
  object Cmd {
    final case class GBEST(options: SimOptions) extends Cmd
    final case class QPSO(options: SimOptions) extends Cmd
  }

  /* CLI flags
   * for new flag:
   * - add to SimOptions
   * - make Options.text value
   * - add to options value
   * - consider adding to output filename
   * - parse logic in perparePSO/makeCombinations
   */
  val problemOpt = Options.text("problem").alias("p")
  val iterOpt = Options.text("iterations").alias("i").map(_.toInt)
  val options = (problemOpt ++ iterOpt).as(SimOptions.apply _)

  val gbestCMD = Command("gbest", options, Args.none).map(Cmd.GBEST)
  val qpsoCMD = Command("qpso", options, Args.none).map(Cmd.QPSO)

  val runCommand: Command[Cmd] =
    Command("run", Options.none, Args.none)
      .subcommands(gbestCMD, qpsoCMD)

  val app =
    CliApp.make(
      name = "NNEPSO",
      version = "",
      summary = text(
        "Neural Network Ensembles with Particle Swarm optimization\n" +
          "example from sbt REPL: run gbest -p f1"
      ),
      command = runCommand
    ) {
      case Cmd.GBEST(options) =>
        val outputFile =
          new java.io.File(
            s"out/gbest${options.iterations}${options.problem}.parquet"
          )
        val combinations = preparePSO(options)

        ZStream
          .mergeAll(threads)(combinations: _*)
          .run(parquetSink(outputFile))

      case Cmd.QPSO(options) =>
        val outputFile =
          new java.io.File(
            s"out/qpso_${options.iterations}_${options.problem}.parquet"
          )
        val combinations = prepareQPSO(options)

        ZStream
          .mergeAll(threads)(combinations: _*)
          .run(parquetSink(outputFile))
    }

  /*
   * Algorithm logic
   * move to different file eventually
   */

  def preparePSO(options: SimOptions) = {
    val problem = makeProblem(options.problem)
    val stdPSOState = (x: Position[Double]) => Mem(x, x.zeroed)

    makeCombinations(
      makeSwarm(bounds, swarmSize, stdPSOState),
      gbpso,
      problem,
      Util.extractSolution[Mem[Double]] // Need to add the type parameter to help the compiler unify types
    ).map(_.take(options.iterations))
  }

  /*
   * QPSO logic
   */
  def prepareQPSO(options: SimOptions) = {
    val problem = makeProblem(options.problem)
    val algStream = Runner.staticAlgorithm("QPSO", qpso)
    val QPSOState = (x: Position[Double]) => QuantumState(x, x.zeroed, 0.0)

    makeCombinations(
      makeSwarm(bounds, swarmSize, QPSOState),
      algStream,
      problem,
      Util.extractSolution[QuantumState]  // Need to add the type parameter to help the compiler unify types
    ).map(_.take(options.iterations))
  }

  def makeCombinations[F[_], A, Out](
      swarm: RVar[F[A]],
      algStream: UStream[Algorithm[Kleisli[Step[*], F[A], F[A]]]],
      probStream: UStream[Problem],
      extractSolution: F[A] => Out
  ): List[ZStream[Any, Exception, cilib.exec.Measurement[Out]]] = {
    // zstream things
    for {
      r <- RNG.initN(n_runs, 123456789L)
    } yield {
      Runner
        .foldStep(
          Comparison.dominance(Min),
          r,
          swarm,
          algStream,
          probStream,
          (x: F[A], _: Eval[NonEmptyVector]) => RVar.pure(x)
        )
        .map(Runner.measure(extractSolution))
    }
  }

  // main entry point
  override def run =
    for {
      args <- ZIOAppArgs.getArgs
      _ <- app.run(args.toList)
    } yield ()
}
