
    package de.upb.crc901.automl.multiclass.evaluablepredicates.mlplan.pp.as.RandomizedLogisticRegression;
    /*
        tol : float, optional, default=1e-3
         tolerance for stopping criteria of LogisticRegression


    */

    import de.upb.crc901.automl.multiclass.evaluablepredicates.mlplan.NumericRangeOptionPredicate;

    public class OptionsFor_RandomizedLogisticRegression_tol extends NumericRangeOptionPredicate {
        
        @Override
        protected double getMin() {
            return 1;
        }

        @Override
        protected double getMax() {
            return 1;
        }

        @Override
        protected int getSteps() {
            return -1;
        }

        @Override
        protected boolean needsIntegers() {
            return false;
        }
    }
    
