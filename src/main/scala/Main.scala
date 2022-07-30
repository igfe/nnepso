import Util._
import cilib._
import cilib.exec._
import cilib.io._
import zio.stream._

object main extends zio.App {
  /*
   * combinations is the main ZStream dependency
   * combinations in turn requires swarm, Runner.staticAlgorithm and problemStream
   */


   def run(args: List[String]) = {
    val threads = 1
    val outputFile = new java.io.File("results/out.parquet")
    println("Preparing to run")
    println(args)
    // problem parameters
    val swarmSize = 20
    val problemDimensions = 5
    val bounds = Interval(-100.0, 100.0) ^ problemDimensions

    // combinations objects
    val swarm = makeSwarm(bounds, swarmSize)
    val problem = makeProblem("f3")
    val cmp = Comparison.dominance(Min)
    val iterations = 100

    val combinations =
      for {
        r <- RNG.initN(1, 123456789L)
      } yield {
        Runner.foldStep(
          cmp,
          r,
          swarm,
          AlgStream("gbest"),
          problem,
          (x: Swarm, _) => RVar.pure(x)
        )
        .map(Runner.measure(extractSolution _))
        .take(iterations) // 1000 iterations
      }

      ZStream.mergeAll(threads)(combinations: _*)
      .run(parquetSink(outputFile))
      .exitCode
    }
  }
