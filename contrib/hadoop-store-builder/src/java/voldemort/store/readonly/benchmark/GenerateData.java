package voldemort.store.readonly.benchmark;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import voldemort.utils.Utils;

/**
 * A test harness that takes as input a text file of keys and generates random
 * data as values. This data is output as a SequenceFile where the key is the
 * given key, and the value is the produced value.
 * 
 * @author jay
 * 
 */
public class GenerateData extends Configured implements Tool {

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new GenerateData(), args);
        System.exit(res);
    }

    public int run(String[] args) throws Exception {
        if(args.length != 3)
            Utils.croak("USAGE: GenerateData input-file output-dir value-size");
        JobConf conf = new JobConf(getConf(), GenerateData.class);
        conf.setJobName("generate-data");

        conf.setOutputKeyClass(Text.class);
        conf.setOutputValueClass(IntWritable.class);

        conf.setMapperClass(GenerateDataMapper.class);
        conf.setReducerClass(IdentityReducer.class);
        conf.setNumReduceTasks(0);

        conf.setInputFormat(TextInputFormat.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);
        conf.setOutputKeyClass(Text.class);
        conf.setOutputValueClass(Text.class);

        Path inputPath = new Path(args[0]);
        FileInputFormat.setInputPaths(conf, inputPath);
        Path outputPath = new Path(args[1]);
        // delete output path if it already exists
        FileSystem fs = outputPath.getFileSystem(conf);
        if(fs.exists(outputPath))
            fs.delete(outputPath, true);
        FileOutputFormat.setOutputPath(conf, outputPath);
        conf.setInt("value.size", Integer.parseInt(args[2]));

        JobClient.runJob(conf);
        return 0;
    }

    public static class GenerateDataMapper extends MapReduceBase implements
            Mapper<LongWritable, Text, Text, Text> {

        private String string;

        public void map(LongWritable lineNumber,
                        Text line,
                        OutputCollector<Text, Text> collector,
                        Reporter reporter) throws IOException {
            collector.collect(line, new Text(string));
        }

        @Override
        public void configure(JobConf job) {
            StringBuilder builder = new StringBuilder();
            int size = job.getInt("value.size", -1);
            for(int i = 0; i < size; i++)
                builder.append('a');
            this.string = builder.toString();
        }
    }

}