package nnepso

import Util._
import Heuristics._
import cilib._
import cilib.exec._
import cilib.io._
import cilib.pso._
import cilib.pso.PSO._
import zio.stream._
import zio.ZIOAppArgs
import zio.cli.HelpDoc.Span.text
import zio.cli._
import zio.optics._

object main extends zio.ZIOAppDefault {

  // Global constants
  val threads = 8
  val swarmSize = 20
  val n_iter = 100
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
    final case class GBEST(problem: String) extends Cmd
    final case class QPSO(problem: String) extends Cmd
  }

  val problemOpt = Options.text("problem").alias("p")

  val gbestCMD = Command("gbest", problemOpt, Args.none)
    .map(Cmd.GBEST)
  val qpsoCMD = Command("qpso", problemOpt, Args.none)
    .map(Cmd.QPSO)

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
      case Cmd.GBEST(problem) =>
        val outputFile =
          new java.io.File(s"out/gbest${n_iter}$problem.parquet")
        val combinations = preparePSO(problem)

        ZStream
          .mergeAll(threads)(combinations: _*)
          .run(parquetSink(outputFile))

      case Cmd.QPSO(problem) =>
        val outputFile =
          new java.io.File(s"out/qpso${n_iter}$problem.parquet")
        val combinations = prepareQPSO(problem)

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
          .take(n_iter) // 1000 iterations
      }
    combinations
  }

  /*
   * QPSO logic
   * Clean up ASAP
   */
  case class QuantumState(
      b: Position[Double],
      v: Position[Double],
      charge: Double
  )

  type QuantumParticle = Particle[QuantumState, Double]

  object QuantumState {
    implicit object QSMemory
        extends HasMemory[QuantumState, Double]
        with HasVelocity[QuantumState, Double]
        with HasCharge[QuantumState] {
      def _memory: Lens[QuantumState, Position[Double]] =
        Lens[QuantumState, Position[Double]](
          state => Right(state.b),
          newB => state => Right(state.copy(b = newB))
        )
      def _velocity: Lens[QuantumState, Position[Double]] =
        Lens[QuantumState, Position[Double]](
          state => Right(state.v),
          newV => state => Right(state.copy(v = newV))
        )
      def _charge: Lens[QuantumState, Double] = Lens[QuantumState, Double](
        state => Right(state.charge),
        newCharge => state => Right(state.copy(charge = newCharge))
      )
    }
  }

  def quantumPSO(
      w: Double,
      c1: Double,
      c2: Double,
      cognitive: Guide[QuantumState, Double],
      social: Guide[QuantumState, Double],
      cloudR: (Position[Double], Position[Double]) => RVar[Double]
  )(implicit
      C: HasCharge[QuantumState],
      V: HasVelocity[QuantumState, Double],
      M: HasMemory[QuantumState, Double]
  ): NonEmptyVector[QuantumParticle] => QuantumParticle => Step[
    QuantumParticle
  ] =
    collection =>
      x =>
        for {
          cog <- cognitive(collection, x)
          soc <- social(collection, x)
          v <- stdVelocity(x, soc, cog, w, c1, c2)
          p <-
            if (C._charge.get(x.state).toOption.get < 0.01) stdPosition(x, v)
            else
              quantum(x.pos, cloudR(soc, cog), (_, _) => Dist.stdUniform)
                .flatMap(replace(x, _))
          p2 <- Step.eval(p)(identity)
          p3 <- updateVelocity(p2, v)
          updated <- updatePBestBounds(p3)
        } yield updated

  // Usage
  val domain: NonEmptyVector[Interval] = Interval(0.0, 100.0) ^ 2

  val qpso: Kleisli[Step, NonEmptyVector[QuantumParticle], NonEmptyVector[
    QuantumParticle
  ]] =
    Kleisli(
      Iteration.sync(
        quantumPSO(
          0.729844,
          1.496180,
          1.496180,
          Guide.pbest,
          Guide.dominance(Selection.star),
          (_, _) => RVar.pure(50.0)
        )
      )
    )

  def qswarm: cilib.RVar[NonEmptyVector[Particle[QuantumState, Double]]] =
    Position
      .createCollection(
        PSO.createParticle(x => Entity(QuantumState(x, x.zeroed, 0.0), x))
      )(domain, positiveInt(40))

  def pop: RVar[NonEmptyVector[QuantumParticle]] =
    qswarm
      .map { coll =>
        val C = implicitly[HasCharge[QuantumState]]
        val chargeLens = Lenses._state[QuantumState, Double] >>> C._charge

        coll.zipWithIndex.map { case (current, index) =>
          chargeLens
            .update(current)(z => if (index % 2 == 1) 0.1 else z)
            .toOption
            .get
        }
      }
      .flatMap(RVar.shuffle)

  def prepareQPSO(pString: String) = {
    val problem = makeProblem(pString)
    val algStream: UStream[Algorithm[
      Kleisli[Step, NonEmptyVector[
        Particle[QuantumState, Double]
      ], NonEmptyVector[Particle[QuantumState, Double]]]
    ]] =
      Runner.staticAlgorithm("QPSO", qpso)

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
            (
                x: NonEmptyVector[Particle[QuantumState, Double]],
                _: Eval[NonEmptyVector]
            ) => RVar.pure(x)
          )
          .map(Runner.measure(extractQSolution _))
          .take(n_iter) // 1000 iterations
      }
    combinations
  }

  type qswarm = NonEmptyVector[Particle[QuantumState, Double]]

  def extractQSolution(collection: qswarm): Results = {
    val fitnessValues = collection.map(x =>
      x.pos.objective
        .flatMap(_.fitness match {
          case Left(f) =>
            f match {
              case Feasible(v) => Some(v)
              case _           => None
            }
          case _ => None
        })
        .getOrElse(Double.PositiveInfinity)
    )

    Results(
      min = fitnessValues.toChunk.min,
      average = fitnessValues.toChunk.reduceLeft(_ + _) / fitnessValues.size
    )
  }
}
