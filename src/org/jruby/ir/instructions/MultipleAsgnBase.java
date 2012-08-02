package org.jruby.ir.instructions;

import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;

import java.util.Map;

// These instructions show up in three contexts:
// - To assign args in parenthesized units: |.., (a,b,c), .. |
// - Regular multiple/parallel assignments: x,y,*z = ...
// - When blocks are inlined, all receive* instructions get
//   converted into multiple-assign instructions
public abstract class MultipleAsgnBase extends Instr implements ResultInstr {
    protected Variable result;
    protected Operand array;
    protected final int index;

    protected MultipleAsgnBase(Operation op, Variable result, Operand array, int index) {
        super(op);
        
        assert result != null : "MultipleAsgnBase result is null";
        
        this.result = result;
        this.array = array;
        this.index = index;
    }

    public Operand[] getOperands() {
        return new Operand[]{array};
    }
    
    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        array = array.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public Operand simplifyAndGetResult(IRScope scope, Map<Operand, Operand> valueMap) {
        return super.simplifyAndGetResult(scope, valueMap);
        // SSS FIXME!  This is buggy code for 1.9 mode
/*
        simplifyOperands(valueMap, false);
        Operand val = array.getValue(valueMap);
        return val.fetchCompileTimeArrayElement(index);
*/
    }
    
    public Operand getArray() {
        return array;
    }
    
    public int getIndex() {
        return index;
    }
}
