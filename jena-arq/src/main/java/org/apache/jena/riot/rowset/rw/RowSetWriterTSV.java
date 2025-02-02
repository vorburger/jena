/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jena.riot.rowset.rw;

import java.io.OutputStream;
import java.io.Writer;
import java.util.List;
import java.util.Objects;

import org.apache.jena.atlas.io.AWriter;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.out.NodeFormatter;
import org.apache.jena.riot.out.NodeFormatterTTL;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.riot.rowset.RowSetWriter;
import org.apache.jena.riot.rowset.RowSetWriterFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.exec.RowSet;
import org.apache.jena.sparql.resultset.ResultSetException;
import org.apache.jena.sparql.util.Context;

public class RowSetWriterTSV implements RowSetWriter {

    public static RowSetWriterFactory factory = lang -> {
        if (!Objects.equals(lang, ResultSetLang.RS_TSV ) )
            throw new ResultSetException("RowSetWriter for TSV asked for a "+lang);
        return new RowSetWriterTSV();
    };

    private static final String NL           = "\n" ;
    private static final String SEP          = "\t" ;

    private static final String headerBytes  = "?_askResult" + NL;
    private static final String yesString    = "true";
    private static final String noString     = "false";

    private RowSetWriterTSV() {}

    @Override
    public void write(OutputStream out, RowSet rowSet, Context context) {
        output(IO.wrapUTF8(out), rowSet);
    }

    @Override
    public void write(Writer out, RowSet rowSet, Context context) {
        output(IO.wrap(out), rowSet);
    }

    @Override
    public void write(OutputStream out, boolean result, Context context) {}

    private static void output(AWriter out, boolean booleanResult) {
        out.write(headerBytes);
        if ( booleanResult )
            out.write(yesString);
        else
            out.write(noString);
        out.write(NL);
        out.flush();
    }

    private static void output(AWriter out, RowSet rowSet) {
        try {
            NodeFormatter formatter = createNodeFormatter();
            String sep = null;
            List<Var> vars = rowSet.getResultVars();

            // writes the variables on the first line
            for ( Var var : vars ) {
                if ( sep != null )
                    out.write(sep);
                else
                    sep = SEP;
                out.write("?");
                out.write(var.getVarName());
            }
            out.write(NL);

            // writes one binding by line
            for ( ; rowSet.hasNext() ; ) {
                sep = null;
                Binding b = rowSet.next();

                for ( Var v : vars ) {
                    if ( sep != null )
                        out.write(sep);
                    sep = SEP;

                    Node n = b.get(v);
                    if ( n != null ) {
                        // This will not include a raw tab.
                        formatter.format(out, n);
                    }
                }
                out.write(NL);
            }
        } finally { out.flush();}
    }

    protected static NodeFormatter createNodeFormatter() {
        // Use a Turtle formatter to format terms
        return new NodeFormatterTTL(null, null);
    }
}
