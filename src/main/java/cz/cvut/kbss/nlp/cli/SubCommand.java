/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.kbss.nlp.cli;


import cz.cvut.kbss.nlp.cli.bk.MyCli;

public enum SubCommand {

    BRAT2GATE_MODULE("brat2gate", Brat2GateCLI.class),
    MY_CLI("mycli", MyCli.class);

    String name;
    Class klass;
    
    private SubCommand(String name, Class klass) {
        this.name = name;
        this.klass = klass;
    }
    
    public Class getAssociatedClass() {
        return klass;
    }
        
    @Override
    public String toString() {
        return name;
    }
}
