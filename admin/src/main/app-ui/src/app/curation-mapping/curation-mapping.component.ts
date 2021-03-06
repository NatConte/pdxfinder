import { Component, OnInit } from '@angular/core';
import {MappingService} from "../mapping.service";
import {ActivatedRoute, Router} from '@angular/router';

@Component({
  selector: 'app-curation-mapping',
  templateUrl: './curation-mapping.component.html',
  styles: [``]
})

export class CurationMappingComponent implements OnInit {

    private unmappedDiagnosisCnt: number;
    private unmappedTreatmentCnt: number;
    private orphanedTerms: number;

    constructor(private _mappingService: MappingService,
                private router: Router,
                private route: ActivatedRoute) { }

    ngOnInit() {

        this._mappingService.getUnmappedTermsByType('diagnosis')
            .subscribe(
                data => {
                    this.unmappedDiagnosisCnt = data['totalElements'];
                }
            );

        this._mappingService.getUnmappedTermsByType('treatment')
            .subscribe(
                data => {
                    this.unmappedTreatmentCnt = data['totalElements'];
                }
            );

        this._mappingService.getTermsByStatus('orphaned')
            .subscribe(
                data => {
                    this.orphanedTerms = data['totalElements'];
                }
            );
    }



    onClick(mapType){

        this.router.navigate([mapType],{relativeTo: this.route})
    }
}




/*

Previous way of counting unmmapped diagnosis terms
                    let myData = data["mappings"]; // This recieves the mappings node of the json in required format
                    // Transform all d mappingValues node objects of each json to array format
                    var count:number = 0;
                    for (var i of myData) {
                        myData[count].mappingValues = Array.of(myData[count].mappingValues);
                        count++;
                    }
 */