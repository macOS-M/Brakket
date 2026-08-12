import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { finalize } from 'rxjs';
import { ProgressionService, Progresion, ElementoProgresion } from '../../services/progression.service';

@Component({ selector:'app-progression-view', standalone:true, imports:[DatePipe], templateUrl:'./progression-view.component.html', styleUrl:'./progression-view.component.scss' })
export class ProgressionViewComponent implements OnInit {
  private readonly service=inject(ProgressionService);
  data:Progresion|null=null; loading=true; busy:number|null=null; error=''; success='';
  ngOnInit():void { this.service.get().subscribe({next:d=>{this.data=d;this.loading=false;},error:()=>{this.error='No se pudo cargar tu progresión.';this.loading=false;}}); }
  redeem(e:ElementoProgresion):void { if(this.busy!==null)return; this.run(e.id,this.service.redeem(e.id),`${e.nombre} fue canjeado.`); }
  apply(e:ElementoProgresion):void { if(this.busy!==null)return; this.run(e.id,this.service.apply(e.id),`${e.nombre} ahora aparece en tu perfil.`); }
  remove(e:ElementoProgresion):void { if(this.busy!==null)return; this.run(e.id,this.service.remove(e.id),`${e.nombre} fue retirado de tu perfil.`); }
  private run(id:number,request:ReturnType<ProgressionService['get']>,message:string):void { this.busy=id;this.error='';this.success='';request.pipe(finalize(()=>this.busy=null)).subscribe({next:d=>{this.data=d;this.success=message;},error:err=>this.error=err?.error?.message??'No se pudo completar la operación.'}); }
  label(tipo:string):string { return ({TITULO:'Título',MARCO:'Marco',INSIGNIA:'Insignia'} as Record<string,string>)[tipo]??tipo; }
}
