package ch.so.agi.gretl.steps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Map;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;

public class S3UploadStep {
    private GretlLogger log;
    private String taskName;

    public S3UploadStep() {
        this(null);
    }
    
    public S3UploadStep(String taskName) {
        if (taskName == null) {
            taskName = S3UploadStep.class.getSimpleName();
        } else {
            this.taskName = taskName;
        }
        this.log = LogEnvironment.getLogger(this.getClass());
    }

    public void execute(String accessKey, String secretKey, String sourceObject, String bucketName, String s3EndPoint, String s3Region, String acl, Map<String, String> metaData) throws FileNotFoundException {        
        log.lifecycle(String.format("Start S3UploadStep(Name: %s SourceObject: %s BucketName: %s S3EndPoint: %s S3Region: %s ACL: %s MetaData: %s)", taskName,
                sourceObject, bucketName, s3EndPoint, s3Region, acl, metaData));
        
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        AmazonS3 s3client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new EndpointConfiguration(s3EndPoint, s3Region))
                .withCredentials(new AWSStaticCredentialsProvider(credentials)).build();

        int uploadedFiles = 0;
        
        File sourceObjectFile = new File(sourceObject);
        if (sourceObjectFile.isDirectory()) {
            File directoryPath = new File(sourceObject);
            String filesList[] = directoryPath.list();
            for(String fileName : filesList) {
                File file = Paths.get(sourceObject, fileName).toFile();
                if (file.isDirectory()) {
                    continue;
                }
                
                ObjectMetadata objectMetadata = new ObjectMetadata();
                objectMetadata.setContentLength(file.length());
                for (Map.Entry<String,String> entry : metaData.entrySet()) {
                    objectMetadata.addUserMetadata(entry.getKey(), entry.getValue());
                } 
                
                InputStream inputStream = new FileInputStream(file);
                PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, inputStream, objectMetadata);
                
                s3client.putObject(putObjectRequest.withCannedAcl(CannedAccessControlList.valueOf(acl)));
                uploadedFiles++;                
             }
        } else {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(sourceObjectFile.length());
            for (Map.Entry<String,String> entry : metaData.entrySet()) {
                objectMetadata.addUserMetadata(entry.getKey(), entry.getValue());
            } 
            
            InputStream inputStream = new FileInputStream(sourceObjectFile);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, sourceObjectFile.getName(), inputStream, objectMetadata);

            s3client.putObject(putObjectRequest.withCannedAcl(CannedAccessControlList.valueOf(acl)));
            uploadedFiles++;                            
        }

        log.lifecycle(taskName + ": " + uploadedFiles + " Files have been uploaded to: "+bucketName+".");
    }
}
