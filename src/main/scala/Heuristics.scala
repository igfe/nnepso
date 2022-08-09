package nnepso

import cilib.NonEmptyVector
import cilib._
import cilib.exec._
import cilib.pso.Defaults._
import cilib.pso.PSO._
import cilib.pso._
import zio.optics._
import zio.prelude.newtypes.Natural

object Heuristics {

  // def AlgStream(name: String) = {
  //   val w = 0.729844
  //   val c1 = 1.496180
  //   val c2 = 1.496180
  //   name match {
  //     case "gbest" => { // TODO implement pattern matching by name, fail if not found
  //       val gbpso = gbest(
  //         w,
  //         c1,
  //         c2,
  //         Guide.pbest[Mem[Double], Double],
  //         Guide.gbest[Mem[Double]]
  //       )
  //       Runner.staticAlgorithm(name, Kleisli(Iteration.sync(gbpso)))
  //     }
  //     // case "quantum" => {
  //     //   val qpso =
  //     //     Iteration.sync(
  //     //       quantumPSO(
  //     //         w,
  //     //         c1,
  //     //         c2,
  //     //         Guide.pbest,
  //     //         Guide.dominance(Selection.star),
  //     //         (_, _) => RVar.pure(50.0)
  //     //       )
  //     //     )
  //     //   Runner.staticAlgorithm(name, Kleisli(Iteration.sync(qpso)))
  //     // }
  //     case _ => {
  //       throw new Exception("invalid algorithm name")
  //     }
  //   }
  // }

  val gbpso = Runner.staticAlgorithm("gbest", Kleisli(Iteration.sync(gbest(
    0.729844,
    1.496180,
    1.496180,
    Guide.pbest[Mem[Double], Double],
    Guide.gbest[Mem[Double]]
  ))))

  def makeSwarm[S](
      bounds: NonEmptyVector[Interval],
      swarmSize: Int,
      stateFunc: Position[Double] => S
  ): RVar[NonEmptyVector[Particle[S, Double]]] = {
    val size = Natural.make(swarmSize).toOption.get

    Position.createCollection(
      PSO.createParticle(x => Entity(stateFunc(x), x))
    )(bounds, size)
  }

  type qswarm = NonEmptyVector[Particle[QuantumState, Double]]

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

  def qswarm: cilib.RVar[qswarm] =
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
}
