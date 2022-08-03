import Util._
import Heuristics._
import cilib._
import cilib.exec._
import cilib.io._
import zio.stream._

object main extends zio.App {
  def run(args: List[String]) = {
    val threads = 1
    println("Preparing to run")
    println(args)
    // problem parameters
    val swarmSize = 20
    val iterations = 100
    val problemDimensions = 5
    val bounds = Interval(-100.0, 100.0) ^ problemDimensions
    val algname = "gbest" //change to "quantum"

    // combinations objects
    val problem = makeProblem("f3")
    val alg = AlgStream(algname)

    // zstream things
    val outputFile = new java.io.File("results/out.parquet")
    val combinations =
      for {
        r <- RNG.initN(1, 123456789L)
      } yield {
        Runner
          .foldStep(
            Comparison.dominance(Min),
            r,
            makeSwarm(bounds, swarmSize, algname),
            alg,
            problem,
            (x: Swarm, _) => RVar.pure(x)
          )
          .map(Runner.measure(extractSolution _))
          .take(iterations) // 1000 iterations
      }

    ZStream
      .mergeAll(threads)(combinations: _*)
      .run(parquetSink(outputFile))
      .exitCode
  }
}
