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
public class VectorAverage implements Reduce.ReduceFunction<Vector> {

    @Inject
    public VectorAverage() {
    }

    private boolean isNullVector(Vector vector) {
        for (int i = 0; i < vector.size(); i++) {
            if (vector.get(i) != 0.0)
                return false;
        }
        return true;
    }
    @Override
    public Vector apply(Iterable<Vector> elements) {
        double num_total = 0;
        Vector result = new DenseVector(6);
        for (Vector element : elements) {
            if(!isNullVector(element)) {
                result.add(element);
                num_total++;
            }
        }
        System.out.print("The Number of Received vectors: " + num_total+"\n");
        int i = 0;
        for (; i<6; i++) {
            System.out.print("Sum of theta[" + i + "] =" + result.get(i) + "\n");
            result.set(i, result.get(i) / num_total);
        }
        return result;
    }

}