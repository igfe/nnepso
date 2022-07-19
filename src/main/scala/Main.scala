import cilib._

object Main {
  def r_var_test(rng: RNG): Unit = {
    val listOfInts = RVar.ints(12)

    val doubledListOfInts =
      for {
        list <- listOfInts
      } yield list.map(x => x * 2)

    val result = doubledListOfInts.run(rng)
    println(result)
  }

  def main(args: Array[String]): Unit = {
    val rng = RNG.init(1234)
    r_var_test(rng)
  }
}