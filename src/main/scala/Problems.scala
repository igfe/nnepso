// package nnepso

// import cilib._
// import cilib.exec._
// import _root_.benchmarks._
// import cilib._
// import zio.prelude._
// import zio.prelude.{Comparison => _, _}

// object Problems {
//   /* Convert the NonEmtpyVector into a AtLeast2List structure which
//    * guaraantees that there are 2 or more elements; fail otherwise
//    */
//   def mkAtLeast2List(x: NonEmptyVector[Double]) =
//     AtLeast2List.make(x) match {
//       case ZValidation.Failure(_, e @ _) =>
//         sys.error("Input vector requires at least 2 elements")
//       case ZValidation.Success(_, result) => result
//     }

//   def makeProblem(name: String) = {
//     val p = Eval.unconstrained((x: NonEmptyVector[Double]) => {
//       Feasible(name match {
//         case "f1" => benchmarks.cec.cec2005.Benchmarks.f1(x)
//         case "f3" => benchmarks.cec.cec2005.Benchmarks.f3(mkAtLeast2List(x))
//         case _    => throw new Exception("invalid problem name")
//       })
//     })
//     Runner.staticProblem(name, p)
//   }
// }
