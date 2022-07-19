import cilib._
import cilib.exec._
import cilib.pso.Defaults._
import cilib.pso._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric._
import scalaz.effect._
import scalaz.stream.{Process, _}
import spire.implicits._
import spire.math.Interval

object Main extends SafeApp {
  // General config
  val iterations = 100
  val independentRuns = 2
  val cores = 8
  val outputFile = "results.csv"
  val numberOfParticles: Int Refined Positive = 10
  val bounds = Interval(-5.12, 5.12) ^ 30 // The bounds of our search space(s)

  // Our algorithm
  val cognitive = Guide.pbest[Mem[Double], Double]
  val social = Guide.gbest[Mem[Double]]
  val gbestPSO = gbest(0.729844, 1.496180, 1.496180, cognitive, social)
  val algorithmName: String Refined NonEmpty = "GBest PSO"
  val swarm = Position.createCollection(PSO.createParticle(x => Entity(Mem(x, x.zeroed), x)))(bounds, numberOfParticles)



}