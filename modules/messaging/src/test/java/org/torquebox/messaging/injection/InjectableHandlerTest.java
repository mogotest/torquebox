package org.torquebox.messaging.injection;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.torquebox.core.injection.analysis.Injectable;
import org.torquebox.core.injection.analysis.InjectableHandlerRegistry;
import org.torquebox.core.injection.analysis.InjectionAnalyzer;
import org.torquebox.core.injection.analysis.InjectionRubyByteCodeVisitor;
import org.torquebox.core.injection.jndi.JNDIInjectable;
import org.torquebox.core.injection.jndi.JNDIInjectableHandler;
import org.torquebox.core.runtime.RubyRuntimeMetaData.Version;

public class InjectableHandlerTest {
    

    private InjectionAnalyzer analyzer;
    private InjectableHandlerRegistry registry;
    private InjectionRubyByteCodeVisitor visitor;

    protected String readScript(String name) throws IOException {
        InputStream in = getClass().getResourceAsStream( name );
        InputStreamReader reader = new InputStreamReader( in );
        BufferedReader buffered = new BufferedReader( reader );

        String line = null;
        StringBuilder script = new StringBuilder();

        while ((line = buffered.readLine()) != null) {
            script.append( line );
            script.append( "\n" );
        }

        return script.toString();
    }

    @Before
    public void setUpAnalyzer() {
        this.registry = new InjectableHandlerRegistry();
        this.registry.addInjectableHandler( new JNDIInjectableHandler() );
        this.registry.addInjectableHandler( new TopicInjectableHandler() );
        this.registry.addInjectableHandler( new QueueInjectableHandler() );
        this.analyzer = new InjectionAnalyzer( this.registry );
        this.visitor = new InjectionRubyByteCodeVisitor( this.analyzer );
    }
    
    @After
    public void tearDownAnalyzer() {
        this.analyzer.destroy();
    }

    @Test
    public void testGenericAnalysis() throws Exception {
        String script = readScript( "generic_injection.rb" );
        analyzer.analyze( "generic_injection.rb", script.toString(), this.visitor, Version.V1_8 );
        
        Set<Injectable> injectables = this.visitor.getInjectables();
        
        assertEquals( 3, injectables.size() );

        assertContains( injectables, JNDIInjectable.class, "jndi", "java:/comp/whatever", "java:/comp/whatever" );
        assertContains( injectables, DestinationInjectable.class, "queue", "/queues/mine", "/queues/mine" );
        assertContains( injectables, DestinationInjectable.class, "topic", "/topics/yours", "/topics/yours" );
    }
    
    protected void assertContains(Set<Injectable> actual, Class injectableClass, String type, String name, String key) {
        int numFound = 0;

        for (Injectable each : actual) {
            if (injectableClass.isInstance( each )) {
                if (each.getType().equals( type )) {
                    if (each.getName().equals( name )) {
                        if (each.getKey().equals( key )) {
                            ++numFound;
                        }
                    }
                }
            }
        }

        if (numFound == 1) {
            return;
        }

        if (numFound > 1) {
            fail( "Too many found: " + injectableClass.getName() + "/" + type + "/" + name + "/" + key + ": " + numFound );
            return;
        }
        fail( "Expected but not found: " + injectableClass.getName() + "/" + type + "/" + name + "/" + key );

    }



}
