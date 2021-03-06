package ch.so.agi.gretl.tasks;

import ch.ehi.basics.settings.Settings;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.tasks.impl.AbstractValidatorTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.interlis2.validator.Validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.interlis.iox_j.plugins.IoxPlugin;
import ch.interlis.iox_j.validator.InterlisFunction;


public class IliValidator extends AbstractValidatorTask {
    private GretlLogger log;

    @TaskAction
    public void validate() {
        log = LogEnvironment.getLogger(IliValidator.class);

        if (dataFiles == null || dataFiles.size() == 0) {
            return;
        }
        List<String> files = new ArrayList<String>();
        for (Object fileObj : dataFiles) {
            String fileName = this.getProject().file(fileObj).getPath();
            files.add(fileName);
        }

        Settings settings = new Settings();
        initSettings(settings);
        
        List<String> userFunctionList = new ArrayList<String>();
        userFunctionList.add("ch.so.agi.ilivalidator.ext.IsHttpResourceIoxPlugin");
        userFunctionList.add("ch.so.agi.ilivalidator.ext.AreaIoxPlugin");
        userFunctionList.add("ch.so.agi.ilivalidator.ext.IsValidDocumentsCycleIoxPlugin");
        userFunctionList.add("ch.so.agi.ilivalidator.ext.IsHttpResourceFromOerebMultilingualUriIoxPlugin");
        
        Map<String,Class> userFunctions = new HashMap<String,Class>();
        try {
            for (String userFunction : userFunctionList) {
                Class clazz = Class.forName(userFunction);
                IoxPlugin plugin=(IoxPlugin)clazz.newInstance();
                userFunctions.put(((InterlisFunction) plugin).getQualifiedIliName(), clazz); 
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            log.info("Class not found");
        } catch (InstantiationException e) {
            e.printStackTrace();
            log.error("cannot instantiate class", e);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            log.error("Class not accessible", e);            
        }
        
        settings.setTransientObject(ch.interlis.iox_j.validator.Validator.CONFIG_CUSTOM_FUNCTIONS, userFunctions);

        validationOk = new Validator().validate(files.toArray(new String[files.size()]), settings);
        if (!validationOk && failOnError) {
            throw new TaskExecutionException(this, new Exception("validation failed"));
        }
    }
}
