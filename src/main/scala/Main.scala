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
  val threads = 1
  val swarmSize = 20
  val iterations = 100
  val problemDimensions = 5
  val bounds = Interval(-100.0, 100.0) ^ problemDimensions

  sealed trait Cmd
  object Cmd {
    final case class PSO(problem: String) extends Cmd
    final case class QPSO(problem: String) extends Cmd
  }

  val problemOpt = Options.text("problem")

  val pso = Command("pso", problemOpt, Args.none)
    .map(Cmd.PSO)
  val qpso = Command("qpso", problemOpt, Args.none)
    .map(Cmd.QPSO)

  val runCommand: Command[Cmd] =
    Command("run", Options.none, Args.none)
      .subcommands(pso, qpso)

  val app =
    CliApp.make(
      name = "",
      version = "",
      summary = text("this is a summary"),
      command = runCommand
    ) {
      case Cmd.PSO(problem) =>
        val outputFile = new java.io.File("out/results.parquet")
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

  def preparePSO(problem: String) = {
    val problem = makeProblem("f3")
    val alg = AlgStream("gbest")

    val stdPSOState = (x: Position[Double]) => Mem(x, x.zeroed)

    // zstream things
    val combinations =
      for {
        r <- RNG.initN(1, 123456789L)
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
}
