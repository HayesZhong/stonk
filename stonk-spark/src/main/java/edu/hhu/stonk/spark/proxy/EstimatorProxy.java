package edu.hhu.stonk.spark.proxy;

import edu.hhu.stonk.spark.task.TaskMLalgorithm;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.lang.reflect.Method;

/**
 * Estimator Proxy
 *
 * @author hayes, @create 2017-12-12 16:18
 **/
public class EstimatorProxy extends MLAlgorithmProxy {

    EstimatorProxy(TaskMLalgorithm mlAlgo) throws Exception {
        super(mlAlgo);
    }

    public ModelProxy fit(Dataset<Row> dataset) throws Exception {
        Method method = algoClazz.getMethod("fit");
        Class modelClass = Class.forName(this.desc.getClassName() + "Model");
        return new ModelProxy(method.invoke(algo, dataset), modelClass);
    }
}
