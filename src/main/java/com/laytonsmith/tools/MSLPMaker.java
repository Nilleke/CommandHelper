package com.laytonsmith.tools;


import static com.laytonsmith.PureUtilities.TermColors.*;
import com.laytonsmith.PureUtilities.ZipMaker;
import com.laytonsmith.core.AliasCore;
import com.laytonsmith.core.MethodScriptCompiler;
import com.laytonsmith.core.Script;
import com.laytonsmith.core.exceptions.ConfigCompileException;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author layton
 */
public class MSLPMaker {
    
    public static void start(String path){
        File start = new File(path);
        if(!start.exists()){
            System.err.println("The specified file does not exist!");
            return;
        }
        
        File output = new File(start.getParentFile(), start.getName() + ".mslp");
        if(output.exists()){
            pl("The file " + output.getName() + " already exists, would you like to overwrite? (Y/N)");
            String overwrite = prompt();
            if(!overwrite.equalsIgnoreCase("y")){
                return;
            }
        }
        //First attempt to compile it, and make sure it doesn't fail
        AliasCore.LocalPackage localPackage = new AliasCore.LocalPackage();
        AliasCore.GetAuxAliases(start, localPackage);      
        boolean error = false;
        for(AliasCore.LocalPackage.FileInfo fi : localPackage.getMSFiles()){
            try{
                MethodScriptCompiler.compile(MethodScriptCompiler.lex(fi.contents(), fi.file()));
            } catch(ConfigCompileException e){
                error = true;
                ConfigRuntimeException.React(e, "Compile error in script. Compilation will attempt to continue, however.", null);
            }
        }
        List<Script> allScripts = new ArrayList<Script>();
        for(AliasCore.LocalPackage.FileInfo fi : localPackage.getMSAFiles()){
            List<Script> tempScripts;
            try{
                tempScripts = MethodScriptCompiler.preprocess(MethodScriptCompiler.lex(fi.contents(), fi.file()));
                for (Script s : tempScripts) {
                    try {
                        s.compile();
                        s.checkAmbiguous(allScripts);
                        allScripts.add(s);
                    } catch (ConfigCompileException e) {
                        error = true;
                        ConfigRuntimeException.React(e, "Compile error in script. Compilation will attempt to continue, however.", null);
                    }
                }
            } catch(ConfigCompileException e){
                error = true;
                ConfigRuntimeException.React(e, "Could not compile file " + fi.file() + " compilation will halt.", null);
            }
        }
        
        if(!error){
            ZipMaker.MakeZip(start, output.getName());

            pl(GREEN + "The MSLP file has been created at " + output.getAbsolutePath() + reset());
        } else {
            pl(RED + "MSLP file has not been created due to compile errors. Correct the errors, and try again." + reset());
        }
    }
}
