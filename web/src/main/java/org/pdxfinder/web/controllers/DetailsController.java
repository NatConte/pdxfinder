package org.pdxfinder.web.controllers;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.commons.collections4.ListUtils;
import org.pdxfinder.services.DetailsService;
import org.pdxfinder.services.PdfService;
import org.pdxfinder.services.dto.DetailsDTO;
import org.pdxfinder.services.pdf.Label;
import org.pdxfinder.services.pdf.PdfHelper;
import org.pdxfinder.services.pdf.Report;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/*
 * Created by abayomi on 09/05/2018.
 */
@Controller
public class DetailsController {


    private DetailsService detailsService;

    @Autowired
    PdfService pdfService;


    @Autowired
    public DetailsController(DetailsService detailsService) {
        this.detailsService = detailsService;
    }


    @RequestMapping(value = "/pdx/{dataSrc}/{modelId:.+}")
    public String details(Model model,
                          @PathVariable String dataSrc,
                          @PathVariable String modelId,
                          @RequestParam(value = "page", defaultValue = "0") Integer page,
                          @RequestParam(value = "size", defaultValue = "15000") Integer size) {

        model.addAttribute("data", detailsService.getModelDetails(dataSrc, modelId));
        return "details";
    }


    @RequestMapping(method = RequestMethod.GET, value = "/pdx/{dataSrc}/{modelId}/{dataType}/export")
    @ResponseBody
    public String download(HttpServletResponse response,
                           @PathVariable String dataSrc,
                           @PathVariable String modelId,
                           @PathVariable String dataType) {

        List<List<String>> variationDataCSV = detailsService.getVariationDataByMolcharTypeCSV(dataSrc, modelId, dataType);

        CsvMapper mapper = new CsvMapper();
        CsvSchema.Builder builder = CsvSchema.builder();

        List<String> csvHead = detailsService.getCsvHead(dataType);

        for (String head : csvHead){
            builder.addColumn(head);
        }
        CsvSchema  schema = builder.build().withHeader();


        String output = "CSV output";
        try {
            output = mapper.writer(schema).writeValueAsString(variationDataCSV);
        } catch (JsonProcessingException e) {}

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=pdxfinder.org_"+dataType + dataSrc + "_" + modelId + ".csv");
        try{
            response.getOutputStream().flush();
        }catch (Exception e){

        }
        return output;


    }


    @GetMapping("/pdx/{dataSrc}/{modelId:.+}/pdf")
    public String pdfView(Model model, HttpServletResponse response,HttpServletRequest request,
                          @PathVariable String dataSrc,
                          @PathVariable String modelId,
                          @RequestParam(value = "option", defaultValue = "download") String option) {

        Report report = new Report();
        PdfHelper pdfHelper = new PdfHelper();

        DetailsDTO detailsDTO = detailsService.getModelDetails(dataSrc, modelId);

        String modelUrl = Label.WEBSITE + request.getRequestURI();

        modelUrl = modelUrl.substring(0, modelUrl.length() - 4);

        report.setFooter(pdfService.generateFooter());
        report.setContent(pdfService.generatePdf(detailsDTO, modelUrl));
        report.setStyles(pdfHelper.getStyles());

        model.addAttribute("report", report);
        model.addAttribute("option", option);
        model.addAttribute("modelId", detailsDTO.getModelId());

        return "pdf-generator";
    }


}

