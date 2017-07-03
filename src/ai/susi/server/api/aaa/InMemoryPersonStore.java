package ai.susi.server.api.aaa;

import org.glassfish.hk2.api.PostConstruct;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author arbocdi
 */
public class InMemoryPersonStore implements PersonStore,PostConstruct{
    private final Map<String,Person> personsMap = new ConcurrentHashMap();

    public InMemoryPersonStore() {
        System.out.println(this.getClass().getName()+" instance created");
    }
    
    

    @Override
    public void addPerson(Person person) throws Exception {
        this.personsMap.put("", person);
    }

    @Override
    public List<Person> getPersons() throws Exception {
        List<Person> personList = new LinkedList();
        personList.addAll(this.personsMap.values());
        return personList;
    }

    @Override
    public Person findPerson(String email) throws Exception {
        return this.personsMap.get(email);
    }

    @Override
    public void deletePerson(String email) throws Exception {
        this.personsMap.remove(email);
    }

    @Override
    public void postConstruct() {
        for (int i=0;i<10;i++){
            Person person = new Person(UUID.randomUUID().toString(), "arbocdi"+i, "email"+i);
            this.personsMap.put("", person);
        }
    }
}
