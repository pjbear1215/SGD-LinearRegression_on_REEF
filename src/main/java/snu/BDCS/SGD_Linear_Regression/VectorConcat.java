/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package snu.BDCS.SGD_Linear_Regression;

import com.microsoft.reef.io.network.group.operators.Reduce;
import com.microsoft.reef.examples.groupcomm.matmul.DenseVector;
import com.microsoft.reef.examples.groupcomm.matmul.Vector;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * exampleSet Reduce function that concatenates an iterable of vectors into a single
 * vector
 *
 * @author shravan
 */
public class VectorConcat implements Reduce.ReduceFunction<Vector> {

    @Inject
    public VectorConcat() {
    }

    @Override
    public Vector apply(Iterable<Vector> elements) {
        double num_total = 0;
        Vector result = new DenseVector(6);
        for (Vector element : elements) {
            result.add(element);
            num_total++;
        }
        int i = 0;
        for (; i<6; i++)
            result.set(i, result.get(i)/num_total);
        return result;
    }

}