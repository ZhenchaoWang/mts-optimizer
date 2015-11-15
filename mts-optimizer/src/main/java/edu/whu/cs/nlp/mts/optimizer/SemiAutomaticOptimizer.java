package edu.whu.cs.nlp.mts.optimizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * 半自动最佳摘要检索
 *
 * @author ZhenchaoWang 2015年11月15日13:10:29
 *
 */
public class SemiAutomaticOptimizer {

    private static Logger log = Logger.getLogger(SemiAutomaticOptimizer.class);

    public static void main(String[] args) {

        if(args == null || args.length != 3) {
            System.err.println("参数错误！");
            return;
        }

        String topicDir = args[0];
        String rougeDir = args[1];
        String filename = args[2];

        File file = new File(topicDir);
        File[] summaryFiles = file.listFiles();

        File destFile = new File(rougeDir + "/peers/" + filename);

        for (int i = 0; i < summaryFiles.length; i++) {

            File summary = summaryFiles[1];

            log.info("[" + (i + 1) + "/" + summaryFiles.length + "]Processing file: " + summary.getAbsolutePath());

            /**
             * 将当前摘要文件拷贝至rouge评测目录
             */
            try {

                log.info("Copy summaryfile[" + summary.getName() + "] to peer dir!");

                FileUtils.copyFileToDirectory(summary, destFile);

            } catch (IOException e) {

                log.error("Copy summaryfile[" + summary.getName() + "] failed, ignore it!", e);

                continue;
            }

            /**
             * 执行rouge评测程序
             */
            // 先清除scores.out文件
            File scoreOutFile = new File(rougeDir + "/scores.out");
            if(scoreOutFile.exists()) {
                scoreOutFile.delete();
            }

            // 执行rouge评测
            String commond = "perl ROUGE-1.5.5.pl -e /home/eventChain/rouge_eval/data"
                    + " -a -n 2 -x -m -2 4 -u -c 95 -r 1000 -f A -p 0.5 -t 0"
                    + " -d /home/eventChain/rouge_eval/rougejk.in"
                    + " > /home/eventChain/rouge_eval/scores.out";

            String[] commond_rouge = {"/bin/sh", "-c", commond};

            try {
                Process process = Runtime.getRuntime().exec(commond_rouge);
                String errMsg = SemiAutomaticOptimizer.execStreamProcess(process.getErrorStream());
                String outMsg = SemiAutomaticOptimizer.execStreamProcess(process.getInputStream());

                if(StringUtils.isNotBlank(errMsg)){
                    log.warn(errMsg);
                }

                if(StringUtils.isNotBlank(outMsg)){
                    log.info(outMsg);
                }

                if(process.waitFor() != 0){
                    log.error("rouge for file[" + summary.getName() + "] is not normal!");
                }

            } catch (IOException | InterruptedException e) {

                log.error("run rouge for file[" + summary.getName() + "] error!", e);

                continue;

            }

            /**
             * 记录评测结果
             */
            // 加载结果文件
            String resultText = null;
            try {

                resultText = FileUtils.readFileToString(scoreOutFile, "UTF-8");

            } catch (IOException e) {

                log.error("Load rouge result file error for summary[" + file.getName() + "]", e);

                continue;

            }

            StringBuilder sbResults = new StringBuilder();

            // Rouge-1
            String regex_rouge_1 = "3 ROUGE-1 Eval " + filename + "-[A-J].3 R:([0-9\\.]+) P:([0-9\\.]+) F:([0-9\\.]+)";
            Pattern pattern = Pattern.compile(regex_rouge_1);
            Matcher matcher = pattern.matcher(resultText);
            int count = 0;
            float rVal = 0.0f, pVal = 0.0f, fVal = 0.0f;
            while(matcher.find()) {
                if(matcher.groupCount() != 3) {
                    log.error("[" + file.getName() + "]The rouge-1 mathched elements in["+ matcher.group() +"] is " + matcher.groupCount() + ", not 3!");
                    break;
                }
                rVal += Float.parseFloat(matcher.group(1));
                pVal += Float.parseFloat(matcher.group(2));
                fVal += Float.parseFloat(matcher.group(3));
                count++;
            }
            if(count != 4) {
                log.error("[" + file.getName() + "]The rouge-1 mathced elements is " + count + ", not 4!");
                continue;
            }
            sbResults.append(file.getName() + "\tROUGE-1\tR:" + rVal / 4.0f + "\tP:" + pVal / 4.0f + "\tF:" + fVal / 4.0f);

            // Rouge-2
            String regex_rouge_2 = "3 ROUGE-2 Eval " + filename + "-[A-J].3 R:([0-9\\.]+) P:([0-9\\.]+) F:([0-9\\.]+)";
            pattern = Pattern.compile(regex_rouge_2);
            matcher = pattern.matcher(resultText);
            count = 0;
            rVal = 0.0f;
            pVal = 0.0f;
            fVal = 0.0f;
            while(matcher.find()) {
                if(matcher.groupCount() != 3) {
                    log.error("[" + file.getName() + "]The rouge-2 mathched elements in["+ matcher.group() +"] is " + matcher.groupCount() + ", not 3!");
                    break;
                }
                rVal += Float.parseFloat(matcher.group(1));
                pVal += Float.parseFloat(matcher.group(2));
                fVal += Float.parseFloat(matcher.group(3));
                count++;
            }
            if(count != 4) {
                log.error("[" + file.getName() + "]The rouge-2 mathced elements is " + count + ", not 4!");
                continue;
            }
            sbResults.append("\tROUGE-2\tR:" + rVal / 4.0f + "\tP:" + pVal / 4.0f + "\tF:" + fVal / 4.0f);

            // Rouge-SU4
            String regex_rouge_su4 = "3 ROUGE-SU4 Eval " + filename + "-[A-J].3 R:([0-9\\.]+) P:([0-9\\.]+) F:([0-9\\.]+)";
            pattern = Pattern.compile(regex_rouge_su4);
            matcher = pattern.matcher(resultText);
            count = 0;
            rVal = 0.0f;
            pVal = 0.0f;
            fVal = 0.0f;
            while(matcher.find()) {
                if(matcher.groupCount() != 3) {
                    log.error("[" + file.getName() + "]The rouge-SU4 mathched elements in["+ matcher.group() +"] is " + matcher.groupCount() + ", not 3!");
                    break;
                }
                rVal += Float.parseFloat(matcher.group(1));
                pVal += Float.parseFloat(matcher.group(2));
                fVal += Float.parseFloat(matcher.group(3));
                count++;
            }
            if(count != 4) {
                log.error("[" + file.getName() + "]The rouge-SU4 mathced elements is " + count + ", not 4!");
                continue;
            }
            sbResults.append("\tROUGE-2\tR:" + rVal / 4.0f + "\tP:" + pVal / 4.0f + "\tF:" + fVal / 4.0f + "\n");

            // 保存结果
            File saveFile = new File(rougeDir + "/optimizeResults/" + filename + ".result");
            try {
                FileUtils.writeStringToFile(saveFile, sbResults.toString(), Charset.forName("UTF-8"), true);
            } catch (IOException e) {

                log.error("Save result[" + saveFile.getAbsolutePath() + "] error!", e);

            }

        }

    }

    /**
     * 执行命令过程中的输出处理
     * @param in
     * @return
     * @throws IOException
     */
    private static String execStreamProcess(InputStream in) throws IOException{

        String output = "";

        if(null == in) {
            return output;
        }

        BufferedReader br = null;
        try{
            br = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb_tmp = new StringBuilder();
            String line = null;
            while((line = br.readLine()) != null){
                sb_tmp.append(line + "\n");
            }
            if(sb_tmp.length() > 0){
                output = sb_tmp.toString();
            }
        } finally {
            if(null != br) {
                br.close();
            }
        }

        return output;

    }

}
