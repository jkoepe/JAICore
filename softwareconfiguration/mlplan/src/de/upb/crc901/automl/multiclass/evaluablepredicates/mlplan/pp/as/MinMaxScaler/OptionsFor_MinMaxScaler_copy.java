package de.upb.crc901.automl.multiclass.evaluablepredicates.mlplan.pp.as.MinMaxScaler;

    import java.util.Arrays;
    import java.util.List;

import de.upb.crc901.automl.multiclass.evaluablepredicates.mlplan.OptionsPredicate;
    /*
        copy : boolean, optional, default True
        Set to False to perform inplace row normalization and avoid a
        copy (if the input is already a numpy array).

    Attributes
    
    */
    public class OptionsFor_MinMaxScaler_copy extends OptionsPredicate {
        
        private static List<Object> validValues = Arrays.asList(new Object[]{"true", "false"});

        @Override
        protected List<? extends Object> getValidValues() {
            return validValues;
        }
    }
    
