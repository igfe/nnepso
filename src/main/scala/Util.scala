import _root_.benchmarks._
import cilib._
import cilib.exec._
import cilib.pso._
import zio.prelude._
import zio.prelude.{Comparison => _, _}

object Util {

  def makeProblem(name: String) = {
    val p = Eval.unconstrained((x: NonEmptyVector[Double]) => {
      Feasible(name match {
        case "f1" => benchmarks.cec.cec2005.Benchmarks.f1(x)
        case "f3" => benchmarks.cec.cec2005.Benchmarks.f3(mkAtLeast2List(x))
        case _    => throw new Exception("invalid problem name")
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

  // A data structure to hold the resulting values.
  // Each class member is mapped to a column within the output file
  final case class Results(min: Double, average: Double)
  type Swarm = NonEmptyVector[Particle[Mem[Double], Double]]

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
