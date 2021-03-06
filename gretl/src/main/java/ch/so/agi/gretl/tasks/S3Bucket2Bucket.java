package ch.so.agi.gretl.tasks;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.steps.S3Bucket2BucketStep;
import ch.so.agi.gretl.steps.S3UploadStep;
import ch.so.agi.gretl.util.TaskUtil;

public class S3Bucket2Bucket extends DefaultTask {
    protected GretlLogger log;

    @Input
    public String accessKey;
    
    @Input
    public String secretKey;
        
    @Input
    public String sourceBucket;
    
    @Input
    public String targetBucket;
    
    @Input
    @Optional
    public String endPoint = "https://s3.amazonaws.com/";
    
    @Input
    public String region = "eu-central-1";
        
    @Input
    @Optional        
    public Map<String,String> metaData = new HashMap<String,String>();
    
    @TaskAction
    public void upload() {
        log = LogEnvironment.getLogger(S3Bucket2Bucket.class);

        if (accessKey == null) {
            throw new IllegalArgumentException("accessKey must not be null");
        }
        if (secretKey == null) {
            throw new IllegalArgumentException("secretKey must not be null");
        }
        if (sourceBucket == null) {
            throw new IllegalArgumentException("sourceBucket must not be null");
        }
        if (targetBucket == null) {
            throw new IllegalArgumentException("targetBucket must not be null");
        }        
        if (region == null) {
            throw new IllegalArgumentException("region must not be null");
        }        
                
        try {
            S3Bucket2BucketStep s3Bucket2Bucket = new S3Bucket2BucketStep();
            s3Bucket2Bucket.execute(accessKey, secretKey, sourceBucket, targetBucket, endPoint, region, metaData);
        } catch (Exception e) {
            log.error("Exception in S3Upload task.", e);
            GradleException ge = TaskUtil.toGradleException(e);
            throw ge;
        }
    }
}
