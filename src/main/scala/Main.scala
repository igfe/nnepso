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
  sealed trait Cmd
  object Cmd {
    final case class GBEST(problem: String, iters: Int) extends Cmd
    final case class QPSO(problem: String, iters: Int) extends Cmd
  }

  val problemOpt = Options.text("problem").alias("p")
  val iterOpt = Options.integer("iterations").alias("i")

  val gbestCMD = Command("gbest", (problemOpt ++ iterOpt), Args.none)
    .map{ case (problem, iterations) => Cmd.GBEST(problem, iterations.toInt) }
  val qpsoCMD = Command("qpso", (problemOpt ++ iterOpt), Args.none)
    .map{ case (problem, iterations) => Cmd.QPSO(problem, iterations.toInt) }

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
      case Cmd.GBEST(problem, n_iter) =>
        val outputFile =
          new java.io.File(s"out/gbest${n_iter}$problem.parquet")
        val combinations = preparePSO(problem, n_iter)

        ZStream
          .mergeAll(threads)(combinations: _*)
          .run(parquetSink(outputFile))

      case Cmd.QPSO(problem, n_iter) =>
        val outputFile =
          new java.io.File(s"out/qpso${n_iter}$problem.parquet")
        val combinations = prepareQPSO(problem, n_iter)

        ZStream
          .mergeAll(threads)(combinations: _*)
          .run(parquetSink(outputFile))
    }

  override def run =
    for {
      args <- ZIOAppArgs.getArgs
      _ <- app.run(args.toList)
    } yield ()

  /*
   * Algorithm logic
   * move to different file eventually
   */

  def preparePSO(pstring: String, n_iter: Int) = {
    val problem = makeProblem(pstring)
    val stdPSOState = (x: Position[Double]) => Mem(x, x.zeroed)

    // zstream things
    val combinations =
      for {
        r <- RNG.initN(n_runs, 123456789L)
      } yield {
        Runner
          .foldStep(
            Comparison.dominance(Min),
            r,
            makeSwarm(bounds, swarmSize, stdPSOState),
            gbpso,
            problem,
            (x: Swarm, _: Eval[NonEmptyVector]) => RVar.pure(x)
          )
          .map(Runner.measure(extractSolution _))
          .take(n_iter)
      }
    combinations
  }

  /*
   * QPSO logic
   */
  def prepareQPSO(pString: String, n_iter: Int) = {
    val problem = makeProblem(pString)
    val algStream = Runner.staticAlgorithm("QPSO", qpso)
    val QPSOState = (x: Position[Double]) => QuantumState(x, x.zeroed, 0.0)

    // zstream things
    val combinations =
      for {
        r <- RNG.initN(n_runs, 123456789L)
      } yield {
        Runner
          .foldStep(
            Comparison.dominance(Min),
            r,
            makeSwarm(bounds, swarmSize, QPSOState),
            algStream,
            problem,
            (x: qswarm, _: Eval[NonEmptyVector]) => RVar.pure(x)
          )
          .map(Runner.measure(extractSolution _))
          .take(n_iter)
      }
    combinations
  }
  // def preparePSO(pstring: String, n_iter: Int) = {
  //   val problem = makeProblem(pstring)
  //   val stdPSOState = (x: Position[Double]) => Mem(x, x.zeroed)

  //   combinations(
  //     makeSwarm(bounds, swarmSize, stdPSOState),
  //     gbpso,
  //     problem,
  //     Util.extractSolution
  //   )
  // }

  // /*
  //  * QPSO logic
  //  */
  // def prepareQPSO(pString: String, n_iter: Int) = {
  //   val problem = makeProblem(pString)
  //   val algStream = Runner.staticAlgorithm("QPSO", qpso)
  //   val QPSOState = (x: Position[Double]) => QuantumState(x, x.zeroed, 0.0)

  //   combinations(
  //     makeSwarm(bounds, swarmSize, QPSOState),
  //     algStream,
  //     problem,
  //     Util.extractSolution
  //   )
  // }

  // def combinations[F[_], A, Out](
  //   swarm: RVar[F[A]],
  //   algStream: UStream[Algorithm[Kleisli[Step[*], F[A], F[A]]]],
  //   probStream: UStream[Problem],
  //   extractSolution: F[A] => Out
  // ) = {
  //   // zstream things
  //   for {
  //     r <- RNG.initN(n_runs, 123456789L)
  //   } yield {
  //     Runner
  //       .foldStep(
  //         Comparison.dominance(Min),
  //         r,
  //         swarm,
  //         algStream,
  //         probStream,
  //         (x: F[A], _: Eval[NonEmptyVector]) => RVar.pure(x)
  //       )
  //       .map(Runner.measure(extractSolution))
  //       .take(n_iter)
  //   }
  // }
}
