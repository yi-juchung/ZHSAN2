package com.zhsan.gameobject;

import com.badlogic.gdx.files.FileHandle;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.zhsan.common.Point;
import com.zhsan.common.exception.FileReadException;
import com.zhsan.common.exception.FileWriteException;
import com.zhsan.gamecomponents.common.XmlHelper;
import com.zhsan.resources.GlobalStrings;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Peter on 7/4/2015.
 */
public class Architecture extends GameObject {

    public static final String SAVE_FILE = "Architecture.csv";

    private GameScenario scenario;

    private String name;
    private String nameImageName;

    private List<Point> location;

    private int architectureKindId = -1;
    private int belongedSectionId = -1;

    private Set<Integer> persons = new HashSet<>();
    private Set<Integer> movingPersons = new HashSet<>();
    private Set<Integer> unhiredPersons = new HashSet<>();
    private Set<Integer> unhiredMovingPersons = new HashSet<>();

    private Architecture(int id) {
        super(id);
    }

    public List<Point> getLocation() {
        return location;
    }

    public ArchitectureKind getKind() {
        return scenario.getArchitectureKinds().get(architectureKindId);
    }

    public static final GameObjectList<Architecture> fromCSV(FileHandle root, @NotNull GameScenario scen) {
        int version = scen.getGameSurvey().getVersion();

        GameObjectList<Architecture> result = new GameObjectList<>();

        FileHandle f = root.child(SAVE_FILE);
        try (CSVReader reader = new CSVReader(new InputStreamReader(f.read(), "UTF-8"))) {
            String[] line;
            int index = 0;
            while ((line = reader.readNext()) != null) {
                index++;
                if (index == 1) continue; // skip first line.

                Architecture data = new Architecture(Integer.parseInt(line[0]));
                if (version == 1) {
                    data.nameImageName = line[1];
                    data.setName(line[2]);
                    data.architectureKindId = Integer.parseInt(line[3]);
                    data.location = Point.fromCSVList(line[7]);
                    data.persons = new HashSet<>(XmlHelper.loadIntegerListFromXml(line[8]));
                    data.movingPersons = new HashSet<>(XmlHelper.loadIntegerListFromXml(line[9]));
                    data.unhiredPersons = new HashSet<>(XmlHelper.loadIntegerListFromXml(line[10]));
                    data.unhiredMovingPersons = new HashSet<>(XmlHelper.loadIntegerListFromXml(line[11]));
                } else {
                    data.nameImageName = line[1];
                    data.setName(line[2]);
                    data.architectureKindId = Integer.parseInt(line[3]);
                    data.location = Point.fromCSVList(line[4]);
                    data.belongedSectionId = Integer.parseInt(line[5]);
                }

                data.scenario = scen;
                result.add(data);
            }
        } catch (IOException e) {
            throw new FileReadException(f.path(), e);
        }

        return result;
    }

    public static final void toCSV(FileHandle root, GameObjectList<Architecture> data) {
        FileHandle f = root.child(SAVE_FILE);
        try (CSVWriter writer = new CSVWriter(f.writer(false, "UTF-8"))) {
            writer.writeNext(GlobalStrings.getString(GlobalStrings.Keys.ARCHITECTURE_SAVE_HEADER).split(","));
            for (Architecture d : data) {
                writer.writeNext(new String[]{
                        String.valueOf(d.getId()),
                        d.nameImageName,
                        d.getName(),
                        String.valueOf(d.architectureKindId),
                        Point.toCSVList(d.location),
                        String.valueOf(d.belongedSectionId)
                });
            }
        } catch (IOException e) {
            throw new FileWriteException(f.path(), e);
        }

    }

    public static final void setup(GameScenario scenario) {
        for (Architecture a : scenario.getArchitectures()) {
            for (Section s : scenario.getSections()) {
                if (s.getArchitectureIds().contains(a.getId())) {
                    a.belongedSectionId = s.getId();
                }
            }
            for (Person p : scenario.getPersons()) {
                if (p.getLocation() == a && p.getState() == Person.State.NORMAL) {
                    if (p.getMovingDays() > 0) {
                        a.movingPersons.add(p.getId());
                    } else {
                        a.persons.add(p.getId());
                    }
                } else if (p.getLocation() == a && p.getState() == Person.State.UNHIRED) {
                    if (p.getMovingDays() > 0) {
                        a.unhiredMovingPersons.add(p.getId());
                    } else {
                        a.unhiredPersons.add(p.getId());
                    }
                }
            }
        }

    }

    public Set<Integer> getPersons() {
        return persons;
    }

    public Set<Integer> getMovingPersons() {
        return movingPersons;
    }

    public Set<Integer> getUnhiredPersons() {
        return unhiredPersons;
    }

    public Set<Integer> getUnhiredMovingPersons() {
        return unhiredMovingPersons;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameImageName() {
        return nameImageName;
    }

    public int getBelongedSectionId() {
        return belongedSectionId;
    }

    public Section getBelongedSection() {
        return scenario.getSections().get(belongedSectionId);
    }

    public Faction getBelongedFaction() {
        return getBelongedSection() == null ? null: getBelongedSection().getBelongedFaction();
    }
}
