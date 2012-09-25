

package com.laytonsmith.core.constructs;

import com.laytonsmith.core.environments.CommandHelperEnvironment;
import com.laytonsmith.core.environments.Environment;

/**
 *
 * @author Layton
 */
public class CArrayReference extends Construct{
    public Construct array;
    public Construct index;
    public IVariable name = null;
    public CArrayReference(Construct array, Construct index, Environment env){
        super("", ConstructType.ARRAY, Target.UNKNOWN);
        this.array = array;
        if(array instanceof CArrayReference){
            this.name = ((CArrayReference)array).name;
        }
        if(!(array instanceof CArray) && !(array instanceof CArrayReference)){
            if(array instanceof IVariable){
                name = (IVariable)array;
                Construct ival = env.getEnv(CommandHelperEnvironment.class).GetVarList().get(name.getName(), name.getTarget()).ival();
                if(ival instanceof CArray){
                    this.array = ival;
                } else {
                    this.array = new CArray(getTarget());
                }
            } else {
                this.array = new CArray(getTarget());
            }
        }
        this.index = index;
    }
    
    @Override
    public String toString(){
        return "(" + array + ") -> " + index;
    }
    
    public Construct getInternalArray(){
        Construct temp = array;
        while(temp instanceof CArrayReference){
            temp = ((CArrayReference)temp).array;
        }
        return temp;
    }
    
    public Construct getInternalIndex(){
        if(!(array instanceof CArrayReference)){
            return index;
        }
        CArrayReference temp = (CArrayReference)array;
        while(temp.array instanceof CArrayReference){
            temp = (CArrayReference)temp.array;
        }
        return temp.index;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }
}
