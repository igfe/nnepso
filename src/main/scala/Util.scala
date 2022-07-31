import _root_.benchmarks._
import cilib._
import cilib.exec._
import cilib.pso._
import cilib.pso.Defaults._
import zio.prelude._
import zio.prelude.newtypes.Natural
import zio.prelude.{Comparison => _, _}

object Util {
  def AlgStream(name: String) = {
    name match {
      case _ => { // TODO implement pattern matching by name, fail if not found
        val gbpso = gbest(
          0.729844,
          1.496180,
          1.496180,
          Guide.pbest[Mem[Double], Double],
          Guide.gbest[Mem[Double]]
        )
        Runner.staticAlgorithm(name, Kleisli(Iteration.sync(gbpso)))
      }
    }
  }

  def makeSwarm(bounds: NonEmptyVector[Interval], swarmSize: Int) = {
    val size = Natural.make(swarmSize).toOption.get
    Position.createCollection(
      PSO.createParticle(x => Entity(Mem(x, x.zeroed), x))
    )(bounds, size)
  }

  def makeProblem(name: String) = {
    // val p = name match {
    //   case "f3" =>
    //     Eval.unconstrained((x: NonEmptyVector[Double]) => {
    //       Feasible(benchmarks.cec.cec2005.Benchmarks.f3(mkAtLeast2List(x)))
    //     })
    //   case _ =>
    //     Eval.unconstrained((x: NonEmptyVector[Double]) => {
    //       Feasible(benchmarks.cec.cec2005.Benchmarks.f3(mkAtLeast2List(x)))
    //     })
    // }
    // case "f3" =>
    val p = Eval.unconstrained((x: NonEmptyVector[Double]) => {
      Feasible(name match {
        case "f3" => benchmarks.cec.cec2005.Benchmarks.f3(mkAtLeast2List(x))
        case _    => benchmarks.cec.cec2005.Benchmarks.f1(x)
      })
    })
    Runner.staticProblem(name, p)
  }

  /* Convert the NonEmtpyVector into a AtLeast2List structure which
   * guaraantees that there are 2 or more elements; fail otherwise
   */
  def mkAtLeast2List(x: NonEmptyVector[Double]) =
    AtLeast2List.make(x) match {
      case ZValidation.Failure(_, e @ _) =>
        sys.error("Input vector requires at least 2 elements")
      case ZValidation.Success(_, result) => result
    }
  type Swarm = NonEmptyVector[Particle[Mem[Double], Double]]

  // A data structure to hold the resulting values.
  // Each class member is mapped to a column within the output file
  final case class Results(min: Double, average: Double)

  def extractSolution(collection: Swarm) = {
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
