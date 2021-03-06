
    package de.upb.crc901.automl.multiclass.evaluablepredicates.mlplan.pp.as.SpectralEmbedding;
    /*
        n_components : integer, default: 2
        The dimension of the projected subspace.


    */

    import de.upb.crc901.automl.multiclass.evaluablepredicates.mlplan.NumericRangeOptionPredicate;

    public class OptionsFor_SpectralEmbedding_n_components extends NumericRangeOptionPredicate {
        
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
            return true;
        }
    }
    
