package org.pdxfinder.mapping;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.pdxfinder.rdbms.dao.MappingEntity;
import org.pdxfinder.services.MappingService;
import org.pdxfinder.services.mapping.MappingEntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;


/*
 * Created by abayomi on 24/07/2019.
 */
@Component
public class ImportMappingsToH2 implements CommandLineRunner{

    @Value("${pdxfinder.root.dir}")
    private String finderRootDir;

    private MappingService mappingService;

    private Logger log = LoggerFactory.getLogger(CreateDirectMappings.class);

    public ImportMappingsToH2(MappingService mappingService) {
        this.mappingService = mappingService;
    }



    @Override
    public void run(String... args) throws Exception {

        OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();
        parser.accepts("initializeMappingDB", "Load Existing Mappings from File to H2 Database");
        OptionSet options = parser.parse(args);

        if (options.has("initializeMappingDB")) {
            log.info("initializing mappings to db");
            mappingFileToH2DB(MappingEntityType.diagnosis);

            mappingFileToH2DB(MappingEntityType.treatment);
        }
    }




    private void mappingFileToH2DB(MappingEntityType type){

        String jsonFile = finderRootDir+"/mapping/"+type+"_mappings.json";

        List<MappingEntity> mappingEntities = mappingService.loadMappingsFromFile(jsonFile);

        mappingService.saveMappedTerms(mappingEntities);

    }






}
