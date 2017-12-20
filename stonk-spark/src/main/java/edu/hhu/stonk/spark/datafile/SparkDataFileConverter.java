package edu.hhu.stonk.spark.datafile;

import edu.hhu.stonk.dao.datafile.DataFile;
import edu.hhu.stonk.dao.datafile.DataFileMapper;
import edu.hhu.stonk.dao.datafile.FieldInfo;
import edu.hhu.stonk.dao.task.StonkTaskInfo;
import edu.hhu.stonk.spark.exception.CantConverException;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.linalg.VectorUDT;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.*;

import java.util.List;

/**
 * 将datafile转换为dataframe
 *
 * @author hayes, @create 2017-12-20 14:58
 **/
public class SparkDataFileConverter {

    public static Dataset<Row> extractDataFrame(StonkTaskInfo taskInfo, JavaSparkContext context) throws Exception {
        DataFileMapper mapper = new DataFileMapper();
        DataFile dataFile = mapper.get(taskInfo.getUname(), taskInfo.getDataFileName());
        return convertToDataFrame(dataFile, context);
    }

    /**
     * 将数据集文件转换为DataFrame
     *
     * @param context
     * @return
     * @throws CantConverException
     */
    public static Dataset<Row> convertToDataFrame(DataFile dataFile, JavaSparkContext context) throws CantConverException {
        SparkSession sparkSession = SparkSession.builder()
                .sparkContext(context.sc())
                .getOrCreate();

        SQLContext sqlContext = new SQLContext(sparkSession);

        switch (dataFile.getDataFileType()) {
            case CSV:
                return csvToDataFrame(dataFile, context, sqlContext);
            case LIBSVM:
                return libsvmToDataFrame(dataFile, sqlContext);
            default:
                throw new CantConverException("不支持的数据集格式");
        }
    }

    private static Dataset<Row> libsvmToDataFrame(DataFile dataFile, SQLContext sqlContext) {
        return sqlContext.read()
                .format("libsvm")
                .load(dataFile.getPath());
    }

    private static Dataset<Row> csvToDataFrame(DataFile dataFile, JavaSparkContext context, SQLContext sqlContext) throws CantConverException {
        StructType schema = getStructType(dataFile);

        JavaRDD<Row> rdd = context.textFile(dataFile.getPath())
                .map(new LineParse(dataFile));
        return sqlContext.createDataFrame(rdd, schema);
//        return sqlContext.read()
//                .format("csv")
//                .option("header", header ? "true" : "false")
//                .option("delimiter", delim)
//                .option("inferSchema", "false")
//                .schema(getStructType())
//                .load(path);
    }

    /**
     * Spark StructType
     *
     * @return
     * @throws CantConverException
     */
    public static StructType getStructType(DataFile dataFile) throws CantConverException {
        List<FieldInfo> fieldInfos = dataFile.getFieldInfos();
        //按照 Index 排序
        fieldInfos.sort((FieldInfo f1, FieldInfo f2) -> f1.getIndex() > f2.getIndex() ? -1 : 1);

        StructField[] fields = new StructField[fieldInfos.size()];
        for (int i = 0; i < fieldInfos.size(); i++) {
            fields[i] = convertToStructField(fieldInfos.get(i));
        }

        return new StructType(fields);
    }


    /**
     * StructField，
     *
     * @return
     * @throws CantConverException
     */
    public static StructField convertToStructField(FieldInfo info) throws CantConverException {
        if (info.getIndex() != -1) {
            return DataTypes.createStructField(info.getName(), sparkDataType(info.getDataType()), info.isNullable());
        } else {
            switch (info.getDataType()) {
                case FieldInfo.STRING_DATATYPE: {
                    return new StructField(info.getName(), DataTypes.createArrayType(DataTypes.StringType), info.isNullable(), Metadata.empty());
                }
                case FieldInfo.DOUBLE_DATATYPE:
                case FieldInfo.INTEGER_DATATYPE:
                case FieldInfo.LONG_DATATYPE: {
                    return new StructField(info.getName(), new VectorUDT(), info.isNullable(), Metadata.empty());
                }
                default:
                    throw new CantConverException("不合法类型");
            }
        }
    }

    /**
     * Spark SQL DataType
     *
     * @return
     */
    public static DataType sparkDataType(String dataType) throws CantConverException {
        switch (dataType) {
            case FieldInfo.DOUBLE_DATATYPE: {
                return DataTypes.DoubleType;
            }
            case FieldInfo.BOOLEAN_DATATYPE: {
                return DataTypes.BooleanType;
            }
            case FieldInfo.INTEGER_DATATYPE: {
                return DataTypes.IntegerType;
            }
            case FieldInfo.STRING_DATATYPE: {
                return DataTypes.StringType;
            }
            case FieldInfo.TIMESTAMP_DATATYPE: {
                return DataTypes.TimestampType;
            }
            case FieldInfo.LONG_DATATYPE: {
                return DataTypes.LongType;
            }
            case FieldInfo.NULL_DATATYPE: {
                return DataTypes.NullType;
            }
            default: {
                throw new CantConverException("不支持的类型");
            }
        }
    }


}
