/**
 *  SusiSkillFile
 *  Copyright 05.07.2019 by Michael Peter Christen, @0rb1t3r
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package ai.susi.mind;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import ai.susi.DAO;
import ai.susi.SusiServer;

public class SusiSkillFile implements Iterator<SusiSkillFile.IntentBlock>, Iterable<SusiSkillFile.IntentBlock> {

    private final ArrayList<IntentBlock> intents;
    private int pos;

    public SusiSkillFile() throws IOException {
        this.intents = new ArrayList<>();
        this.pos = 0;
    }
    
    public SusiSkillFile(final BufferedReader br) throws IOException {
    	this();
        List<List<String>> blocks = textBlockReader(br);
        blocks.forEach(block -> intents.add(new IntentBlock(block)));
    }

    public static SusiSkillFile load(File f) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(f));
        SusiSkillFile skillfile = new SusiSkillFile(br);
        br.close();
        return skillfile;
    }

    public void save(File f) throws IOException {
        FileWriter fw = new FileWriter(f);
        fw.write(this.toString());
        fw.close();
    }

    public static class IntentBlock {

        public final String utterance;
        public final List<String> model;

        public IntentBlock(final List<String> lines) {
            List<String> a = new ArrayList<>();

            // first merge lines with line-glue "\\" into one line
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                assert line.length() > 0;
                if (line.charAt(0) == '\\') {
                    if (a.size() == 0) {
                        a.add(line.substring(1));
                    } else {
                        a.set(a.size() - 1, a.get(a.size() - 1) + ' ' + line.substring(1));
                    }
                } else {
                    if (a.size() == 0) {
                        a.add(line);
                    } else {
                        String lasta = a.get(a.size() - 1);
                        if (lasta.charAt(lasta.length() - 1) == '\\') {
                            a.set(a.size() - 1, lasta.substring(0, lasta.length() - 1) + ' ' + line);
                        } else {
                            a.add(line);
                        }
                    }
                }
            };

            // separate head from utterance and model
            this.model = a.size() < 2 ? null : new ArrayList<>();
            String impl = null;
            for (int i = 0; i < a.size(); i++) {
                String line = a.get(i);
                if (impl == null) {
                    impl = line;
                } else {
                    model.add(line);
                }
            };
            this.utterance = impl;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(utterance).append("\n");
            if (this.model != null) {
                for (String p: model) {
                    sb.append(p).append("\n");
                }
            }
            sb.append("\n");
            return sb.toString();
        }
    }

    public final static Pattern tabPattern = Pattern.compile("\t");

    public static List<List<String>> textBlockReader(final BufferedReader br) throws IOException {
        String line = "";
        List<String> line_bag = new ArrayList<>();
        List<List<String>> blocks = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            line = tabPattern.matcher(righttrim(line)).replaceAll("    ");

            // empty lines close a block
            if (line.length() == 0) {
                if (line_bag.size() > 0) {
                    blocks.add(line_bag);
                    line_bag = new ArrayList<>();
                }
                continue;
            }
            // lines containing "::" at the beginning are always single lines
            if (line.startsWith("::")) {
                if (line_bag.size() > 0) {
                    blocks.add(line_bag);
                    line_bag = new ArrayList<>();
                }
                line_bag.add(line);
                blocks.add(line_bag);
                line_bag = new ArrayList<>();
                continue;
            }

            // comment lines are invisible, as they are not there
            if (line.charAt(0) == '#') continue;

            // all other lines are added to a block
            line_bag.add(line);
        }
        // there might be a unsaved block at the end
        if (line_bag.size() > 0) {
            blocks.add(line_bag);
        }
        return blocks;
    }

    public static String righttrim(String s) {
        int len = s.length();
        while (0 < len && s.charAt(len - 1) <= ' ') len--;
        return len < s.length() ? s.substring(0, len) : s;
    }

    public ArrayList<IntentBlock> getIntents() {
        return this.intents;
    }

    public IntentBlock getIntent(String utterance) {
        for (IntentBlock intent: intents) {
            if (utterance.equals(intent.utterance)) return intent;
        }
        return null;
    }

    public void setIntent(IntentBlock intentNew) {
        for (IntentBlock intent: intents) {
            if (intentNew.utterance.equals(intent.utterance)) {
                intent.model.clear();
                intent.model.addAll(intentNew.model);
                return;
            }
        }
        this.intents.add(intentNew);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (IntentBlock intent: intents) {
            sb.append(intent.toString());
        }
        return sb.toString();
    }

    @Override
    public Iterator<IntentBlock> iterator() {
        return this.intents.iterator();
    }

    @Override
    public boolean hasNext() {
        return pos < this.intents.size();
    }

    @Override
    public IntentBlock next() {
        return this.intents.get(this.pos++);
    }

    public static void main(String[] args) {
        Path data = FileSystems.getDefault().getPath("data");

        try {
            Map<String, String> config = SusiServer.readConfig(data);
            DAO.init(config, data, true);
            File f = new File("conf/os_skills/operation/en/en_0001_foundation.txt");
            SusiSkill.ID skillid = new SusiSkill.ID(f);
            
            String test =
                    "pause | pause that | pause this | pause *\n" + 
                    "!console:\n" + 
                    "{\"actions\":[{\"type\":\"pause\"}]}\n" + 
                    "eol";

            SusiSkillFile ssf = new SusiSkillFile(new BufferedReader(new StringReader(test)));
            System.out.println(ssf.toString());
            SusiSkill ss = new SusiSkill(ssf, skillid, false);
            System.out.println(ss.toJSON().toString(2));

            ssf = SusiSkillFile.load(f);
            System.out.println(ssf.toString());
            ss = new SusiSkill(ssf, skillid, false);
            System.out.println(ss.toJSON().toString(2));
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
