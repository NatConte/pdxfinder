package org.pdxfinder.commands.envload;

/**
 * Created by csaba on 07/05/2019.
 */


import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.neo4j.ogm.json.JSONArray;
import org.neo4j.ogm.json.JSONObject;
import org.pdxfinder.graph.dao.OntologyTerm;
import org.pdxfinder.services.DataImportService;
import org.pdxfinder.services.UtilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

@Component
@Order(value = -65)
public class LoadNCITDrugs implements CommandLineRunner {

    private final static Logger log = LoggerFactory.getLogger(LoadNCIT.class);

    private static final String drugsBranchUrl = "http://purl.obolibrary.org/obo/NCIT_C1908";
    private static final String regimenBranchUrl = "http://purl.obolibrary.org/obo/NCIT_C12218";
    private static final String geneProductBranchUrl = "http://purl.obolibrary.org/obo/NCIT_C26548";

    private static final String ontologyUrl = "https://www.ebi.ac.uk/ols/api/ontologies/ncit/terms/";

    private DataImportService dataImportService;

    @Autowired
    private UtilityService utilityService;

    @Autowired
    public LoadNCITDrugs(DataImportService dataImportService) {
        this.dataImportService = dataImportService;
    }


    private List<String> unlinkedRegimens;
    private List<String> unlinkedRegimensSynonyms;
    private List<String> unlinkedRegimensWithReason;
    private List<String> linkedRegimens;
    private Map<String, Set<OntologyTerm>> linkedRegimenLabelsToTerms;


    private Map<String, OntologyTerm> loadedTreatmentTerms;
    private Map<String, OntologyTerm> loadedTreatmentTermsSynonyms;
    private Map<String, OntologyTerm> loadedRegimenTerms;

    @Override
    public void run(String... args) throws Exception {


        //http://www.ebi.ac.uk/ols/api/ontologies/ncit/terms/http%253A%252F%252Fpurl.obolibrary.org%252Fobo%252FNCIT_C7057/hierarchicalChildren
        //http://www.ebi.ac.uk/ols/api/ontologies/doid/terms/http%253A%252F%252Fpurl.obolibrary.org%252Fobo%252FNCIT_C7057/hierarchicalChildren

        OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();
        parser.accepts("loadNCITDrugs", "Load NCIT drugs");
        parser.accepts("loadALL", "Load all, including NCiT drug ontology");
        parser.accepts("loadEssentials", "Loading essentials");
        OptionSet options = parser.parse(args);

        long startTime = System.currentTimeMillis();

        if (options.has("loadNCITDrugs") || options.has("loadALL")  || options.has("loadEssentials")) {

            loadedTreatmentTerms = new HashMap<>();
            loadedTreatmentTermsSynonyms = new HashMap<>();
            loadedRegimenTerms = new HashMap<>();
            linkedRegimenLabelsToTerms = new HashMap<>();

            //log.info("Test: "+getCleanLabel("ifosfamide-platinol-adriamycin liver cancer"));

            log.info("Loading Drugs from NCIT.");
            loadNCITLeafDrugs("treatment", drugsBranchUrl, true);

            log.info("Loading Gene Products from NCIT.");
            loadNCITLeafDrugs("treatment", geneProductBranchUrl, true);


            log.info("Loading regimens from NCIT.");
            loadNCITLeafDrugs("treatment regimen", regimenBranchUrl, false);

            log.info("Linking drug regimens to individual drugs");
            linkRegimens();

            log.info("Saving treatment links to regimens");
            saveRegimensWithTreatments();

        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        int seconds = (int) (totalTime / 1000) % 60;
        int minutes = (int) ((totalTime / (1000 * 60)) % 60);

        log.info(this.getClass().getSimpleName() + " finished after " + minutes + " minute(s) and " + seconds + " second(s)");

    }





    private void linkRegimens() {

        unlinkedRegimens = new ArrayList<>();
        unlinkedRegimensWithReason = new ArrayList<>();
        linkedRegimens = new ArrayList<>();
        unlinkedRegimensSynonyms = new ArrayList<>();

        int regimenNumber = dataImportService.getOntologyTermNumberByType("treatment regimen");
        int batch = 100;

        int i;
        for (i = 0; i < regimenNumber; i += batch) {

            Collection<OntologyTerm> regimens = dataImportService.getAllOntologyTermsByTypeFromTo("treatment regimen", i, batch);

            for(OntologyTerm regimen : regimens){

                linkRegimenToTreatments(regimen);
            }

            log.info("Linked regimens: "+Integer.toString(i + batch));
        }

        for(int j=0; j<unlinkedRegimens.size(); j++){
            System.out.println(unlinkedRegimens.get(j) + " == " +unlinkedRegimensSynonyms.get(j) +"\n");

        }

        System.out.println("***********************************");

        for(int j=0; j<unlinkedRegimensWithReason.size(); j++){
            System.out.println(unlinkedRegimensWithReason.get(j));

        }
        System.out.println("***********************************");

        System.out.println();
        System.out.println("Linked regimens: "+linkedRegimens.size());
        System.out.println("Unlinked regimens: "+unlinkedRegimens.size());



    }

    private void linkRegimenToTreatments(OntologyTerm regimen){


        Set<String> synonyms = regimen.getSynonyms();

        boolean linked = false;


        String synonymCombo = "";

        String treatmentCausedTheFailure = "";

        List<OntologyTerm> slashMatrix = new ArrayList<>();
        List<OntologyTerm> dashMatrix = new ArrayList<>();

        for(String synonym : synonyms){

            String[] synSlashPre = synonym.split("/");
            String[] synDash = synonym.split("-");


            slashMatrix = new ArrayList<>();
            dashMatrix = new ArrayList<>();

            String[] synSlash = splitCombos(synSlashPre);



            synonymCombo += "#slash ";

            for(int i=0; i < synSlash.length; i++){

                String label = getCleanLabel(synSlash[i]);

                if(loadedTreatmentTerms.containsKey(label)){

                    slashMatrix.add(loadedTreatmentTerms.get(label));
                    //log.info(regimen.getLabel() + " label found: "+label);
                }
                else if(loadedTreatmentTermsSynonyms.containsKey(label)){

                    slashMatrix.add(loadedTreatmentTermsSynonyms.get(label));
                }
                else{

                    slashMatrix.add(null);
                }

                synonymCombo += "|"+label+"|";
            }

            if(isAllNotNull(slashMatrix)) {
                linked = true;
                break;
            }

            synonymCombo += "#dash ";
            for(int i=0; i < synDash.length; i++){

                String label = getCleanLabel(synDash[i]);

                if(loadedTreatmentTerms.containsKey(label)){

                    dashMatrix.add(loadedTreatmentTerms.get(label));
                    //log.info(regimen.getLabel() + " label found: "+label);
                }
                else if(loadedTreatmentTermsSynonyms.containsKey(label)){

                    dashMatrix.add(loadedTreatmentTermsSynonyms.get(label));
                }
                else{

                    dashMatrix.add(null);
                }

                synonymCombo += "|"+label+"|";
            }

            if(isAllNotNull(dashMatrix)) {
                linked = true;
                break;
            }

            //check if found an "almost good" match
            if(isAllNotNullButOne(slashMatrix)){

                int index = getNullPosition(slashMatrix);
                treatmentCausedTheFailure = synSlash[index];
            }
            else if(isAllNotNullButOne(dashMatrix)){

                int index = getNullPosition(dashMatrix);
                treatmentCausedTheFailure = synDash[index];
            }

        }

        if(linked){

            linkedRegimens.add(regimen.getLabel());

            if(isAllNotNull(slashMatrix)){

                linkedRegimenLabelsToTerms.put(regimen.getLabel(), new HashSet<>(slashMatrix));
            }
            else if(isAllNotNull(dashMatrix)){

                linkedRegimenLabelsToTerms.put(regimen.getLabel(), new HashSet<>(dashMatrix));
            }

        }
        else{
            if(treatmentCausedTheFailure.equals("")) treatmentCausedTheFailure = "{MULTIPLE ERRORS}";

            unlinkedRegimens.add(regimen.getLabel());
            unlinkedRegimensSynonyms.add(synonymCombo);
            unlinkedRegimensWithReason.add(regimen.getLabel() + " => " + treatmentCausedTheFailure);
        }
    }




    private void loadNCITLeafDrugs(String type, String branchRootUrl, boolean mapSynonyms){

        int totalDrugs = 0;
        int totalPages = 0;
        boolean totalPagesDetermined = false;


        for(int currentPage = 0; currentPage<= totalPages; currentPage++){

            String encodedTermUrl = "";
            try {
                //have to double encode the url to get the desired result
                encodedTermUrl = URLEncoder.encode(branchRootUrl, "UTF-8");
                encodedTermUrl = URLEncoder.encode(encodedTermUrl, "UTF-8");

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            String url = ontologyUrl+encodedTermUrl+"/hierarchicalDescendants?size=500&page="+currentPage;

            String json = utilityService.parseURL(url);

            try {
                JSONObject job = new JSONObject(json);

                String embedded = job.getString("_embedded");

                JSONObject job2 = new JSONObject(embedded);
                JSONArray terms = job2.getJSONArray("terms");

                for (int j = 0; j < terms.length(); j++) {

                    JSONObject term = terms.getJSONObject(j);

                    boolean hasChildren = Boolean.parseBoolean(term.getString("has_children"));

                    if(!hasChildren){

                        OntologyTerm ot = new OntologyTerm();
                        ot.setType(type);
                        ot.setLabel(term.getString("label"));
                        ot.setUrl(term.getString("iri"));
                        ot.setAllowAsSuggestion(false);

                        if(term.has("description")){
                            ot.setDescription(term.getString("description"));
                        }

                        if(term.has("synonyms")) {
                            JSONArray synonyms = term.getJSONArray("synonyms");
                            Set<String> synonymsSet = new HashSet<>();

                            for (int i = 0; i < synonyms.length(); i++) {
                                synonymsSet.add(synonyms.getString(i));
                            }

                            ot.setSynonyms(synonymsSet);

                            if(mapSynonyms){

                                for (int i = 0; i < synonyms.length(); i++) {
                                    loadedTreatmentTermsSynonyms.put(synonyms.getString(i).toLowerCase(), ot);
                                }
                            }
                        }

                        OntologyTerm savedOt = dataImportService.saveOntologyTerm(ot);

                        if(type.equals("treatment")) loadedTreatmentTerms.put(ot.getLabel().toLowerCase(), savedOt);
                        if(type.equals("treatment regimen")) loadedRegimenTerms.put(ot.getLabel().toLowerCase(), savedOt);


                        totalDrugs++;

                    }

                    if(totalDrugs != 0 && totalDrugs % 500 == 0) {
                        log.info("Loaded "+totalDrugs + " drugs from NCIT.");
                    }


                }

                if(!totalPagesDetermined){
                    String page = job.getString("page");
                    JSONObject pageObj = new JSONObject(page);
                    totalPages = Integer.parseInt(pageObj.getString("totalPages")) -1;
                    totalPagesDetermined = true;
                }


            } catch (Exception e) {
                log.error("", e);

            }

        }

        log.info("Finished loading "+totalDrugs+ " drugs from NCIT.");
    }


    private void saveRegimensWithTreatments(){

        for(Map.Entry<String, Set<OntologyTerm>> entry : linkedRegimenLabelsToTerms.entrySet()){

            String regimenLabel = entry.getKey();

            OntologyTerm regimen = loadedRegimenTerms.get(regimenLabel.toLowerCase());

            regimen.setSubclassOf(entry.getValue());
            dataImportService.saveOntologyTerm(regimen);

        }


    }


    private String getCleanLabel(String label){

        String cleanLabel = label.toLowerCase();
        cleanLabel = cleanLabel.replaceAll("regimen", "");
        cleanLabel = cleanLabel.replaceAll("high dose", "");
        cleanLabel = cleanLabel.replaceAll("dose-dense", "");
        cleanLabel = cleanLabel.replaceAll("high-dose", "");
        cleanLabel = cleanLabel.replaceAll("pulse intense", "");
        cleanLabel = cleanLabel.replaceAll("intravenous", "");
        cleanLabel = cleanLabel.replaceAll("oral", "");
        cleanLabel = cleanLabel.replaceAll("modified", "");
        cleanLabel = cleanLabel.replaceAll("hyperfractionated", "");
        cleanLabel = cleanLabel.replaceAll("infusional", "");

        cleanLabel = cleanLabel.replaceAll("([^\\s]+\\s+cancer)", "");

        return cleanLabel.trim();
    }


    private String[] splitCombos(String[] combos){
    //cyclophosphamide followed by paclitaxel + trastuzumab
        List<String> list = new ArrayList<>();

        for(String c: combos){

            if(c.toLowerCase().contains("followed by")){

                String[] followedBy = c.toLowerCase().split("followed by");
                list.add(followedBy[0]);

                if(followedBy[1].contains("+")){

                    String[] plusDrugs = followedBy[1].split("\\+");
                    list.addAll(Arrays.asList(plusDrugs));

                }
                else if(followedBy[1].contains("/")){

                    String[] dashDrugs = followedBy[1].split("/");
                    list.addAll(Arrays.asList(dashDrugs));
                }
                else{
                    list.add(followedBy[1]);
                }

            }
            else{

                list.add(c);

            }
        }

        return list.stream().toArray(String[]::new);

    }

    /**
     * Returns true if there is no null elements in the list
     * Returns false otherwise
     * @param list
     * @return
     */
    private boolean isAllNotNull(List<OntologyTerm> list){

        for(OntologyTerm ot : list){
            if(ot == null) return false;
        }
        return true;
    }

    /**
     * Returns true if in the list has at least 2 elements and
     * there is exactly one null element
     * Returns false otherwise
     * @param list
     * @return
     */
    private boolean isAllNotNullButOne(List<OntologyTerm> list){

        if(list.size() == 1) return false;

        boolean oneNull = false;

        for(OntologyTerm ot : list){

            if(ot == null) {
                //this is the first null in the list
                if(oneNull == false){
                    oneNull = true;
                }
                //this is not the first null in the list
                else{

                    return false;
                }

            }
        }
        return oneNull;
    }


    /**
     * Returns the null element's index in the list
     * @param list
     * @return
     */
    private int getNullPosition(List<OntologyTerm> list){

        for (int i= 0; i < list.size(); i++){

            if(list.get(i) == null) return i;
        }

        return -1;
    }

}
