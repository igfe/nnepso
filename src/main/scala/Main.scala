package nnepso

import Util._
import Heuristics._
import cilib._
import cilib.exec._
import cilib.io._
import zio.stream._
import zio.ZIOAppArgs
import zio.cli.HelpDoc.Span.text
import zio.cli._

object main extends zio.ZIOAppDefault {

  // Global constants
  val threads = 8
  val swarmSize = 20
  val iterations = 1000
  val problemDimensions = 5
  val bounds = Interval(-100.0, 100.0) ^ problemDimensions
  val n_runs = 1

  /*
   * CLI logic
   * for new algorithm: Cmd object, command value, add to subcommand
   */
  sealed trait Cmd
  object Cmd {
    final case class GBEST(problem: String) extends Cmd
    final case class QPSO(problem: String) extends Cmd
  }

  val problemOpt = Options.text("problem").alias("p")

  val gbest = Command("gbest", problemOpt, Args.none)
    .map(Cmd.GBEST)
  val qpso = Command("qpso", problemOpt, Args.none)
    .map(Cmd.QPSO)

  val runCommand: Command[Cmd] =
    Command("run", Options.none, Args.none)
      .subcommands(gbest, qpso)

  val app =
    CliApp.make(
      name = "NNEPSO",
      version = "",
      summary = text("Neural Network Ensembles with Particle Swarm optimization\n" +
        "example from sbt REPL: run gbest -p f1"),
      command = runCommand
    ) {
      case Cmd.GBEST(problem) =>
        val outputFile = new java.io.File(s"out/gbest${iterations}$problem.parquet")
        val combinations = preparePSO(problem)

        ZStream
          .mergeAll(threads)(combinations: _*)
          .run(parquetSink(outputFile))

      case Cmd.QPSO(problem) =>
        sys.error("asd")

    }

  override def run =
    for {
      args <- ZIOAppArgs.getArgs
      _ <- app.run(args.toList)
    } yield ()

  def preparePSO(pstring: String) = {
    val problem = makeProblem(pstring)
    val alg = AlgStream("gbest")

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
            alg,
            problem,
            (x: Swarm, _) => RVar.pure(x)
          )
          .map(Runner.measure(extractSolution _))
          .take(iterations) // 1000 iterations
      }
    combinations
  }

  // def prepareQPSO(problem: String) = {
  //   val problem = makeProblem("f3")
  //   val alg = AlgStream("qpso")

  //   val QPSOState = (x: Position[Double]) => QuantumState(x, x.zeroed, 0.0)

  //   // zstream things
  //   val combinations =
  //     for {
  //       r <- RNG.initN(1, 123456789L)
  //     } yield {
  //       Runner
  //         .foldStep(
  //           Comparison.dominance(Min),
  //           r,
  //           makeSwarm(bounds, swarmSize, QPSOState),
  //           alg,
  //           problem,
  //           (x: Swarm, _) => RVar.pure(x)
  //         )
  //         .map(Runner.measure(extractSolution _))
  //         .take(iterations) // 1000 iterations
  //     }
  //   combinations
  // }
}
