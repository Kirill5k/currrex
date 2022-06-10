package currexx.algorithms.operators

import currexx.algorithms.{DistributedPopulation, EvaluatedPopulation, Fitness, Population}
import currexx.algorithms.collections.*

import scala.annotation.tailrec
import scala.util.Random

trait Selector[I]:
  def selectPairs(population: EvaluatedPopulation[I], populationLimit: Int)(using r: Random): DistributedPopulation[I]

object Selector {
  def rouletteWheelSelector[I]: Selector[I] = new Selector[I] {
    override def selectPairs(population: EvaluatedPopulation[I], populationLimit: Int)(using r: Random): DistributedPopulation[I] = {
      val popByFitness = population
        .sortBy(_._2)(Ordering[Fitness].reverse)

      val fTotal = popByFitness.map(_._2).reduce(_ + _)

      @tailrec
      def go(newPop: Population[I], remPop: EvaluatedPopulation[I], remFitness: Fitness): Population[I] =
        if (remPop.isEmpty || newPop.size >= populationLimit) newPop
        else {
          val ((pickedInd, indFitness), remaining) = pickOne(remPop, remFitness)
          go(pickedInd +: newPop, remaining, remFitness - indFitness)
        }
      go(Vector.empty, popByFitness, fTotal).reverse.pairs
    }

    private def pickOne(
        popByFitness: EvaluatedPopulation[I],
        fTotal: Fitness
    )(using r: Random): ((I, Fitness), EvaluatedPopulation[I]) = {
      var remFitness = BigDecimal(1.0)

      val chance = r.nextDouble()
      val indIndex = LazyList
        .from(popByFitness)
        .map { case (i, f) =>
          val res = (i, remFitness)
          remFitness = remFitness - (f / fTotal).value
          res
        }
        .indexWhere(_._2 < chance, 0) - 1

      if (indIndex >= 0) {
        val ind    = popByFitness(indIndex)
        val remPop = popByFitness.take(indIndex) ++ popByFitness.drop(indIndex + 1)
        (ind, remPop)
      } else {
        (popByFitness.head, popByFitness.tail)
      }
    }
  }
}
