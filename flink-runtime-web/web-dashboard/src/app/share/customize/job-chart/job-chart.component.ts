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

/// <reference path="../../../../../node_modules/@antv/g2/src/index.d.ts" />

import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  HostBinding,
  Input,
  OnDestroy,
  Output,
  ViewChild
} from '@angular/core';
import { Chart } from '@antv/g2';
import * as G2 from '@antv/g2';

@Component({
  selector: 'flink-job-chart',
  templateUrl: './job-chart.component.html',
  styleUrls: ['./job-chart.component.less'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class JobChartComponent implements AfterViewInit, OnDestroy {
  @Input() title: string;
  @Output() closed = new EventEmitter();
  @ViewChild('chart') chart: ElementRef;
  size = 'small';
  chartInstance: Chart;
  data: Array<{ time: number; value: number; type: string }> = [];

  @HostBinding('class.big')
  get isBig() {
    return this.size === 'big';
  }

  refresh(res: { timestamp: number; values: { [id: string]: number } }) {
    this.data.push({
      time: res.timestamp,
      value: res.values[this.title],
      type: this.title
    });

    if (this.data.length > 20) {
      this.data.shift();
    }
    if (this.chartInstance) {
      this.chartInstance.changeData(this.data);
    }
  }

  resize(size: string) {
    this.size = size;
    this.cdr.detectChanges();
    setTimeout(() => this.chartInstance.forceFit());
  }

  close() {
    this.closed.emit(this.title);
  }

  constructor(private cdr: ChangeDetectorRef) {}

  ngAfterViewInit() {
    this.cdr.detach();
    G2.track(false);
    this.chartInstance = new G2.Chart({
      container: this.chart.nativeElement,
      height: 150,
      forceFit: true,
      padding: 'auto'
    });
    this.chartInstance.legend(false);
    this.chartInstance.source(this.data, {
      time: {
        alias: 'Time',
        type: 'time',
        mask: 'HH:mm:ss',
        tickCount: 3
      },
      type: {
        type: 'cat'
      }
    });
    this.chartInstance
      .line()
      .position('time*value')
      .shape('smooth')
      .color('type')
      .size(2)
      .animate({
        update: {
          duration: 0
        }
      });
    this.chartInstance.render();
  }

  ngOnDestroy() {
    if (this.chartInstance) {
      this.chartInstance.destroy();
    }
  }
}
