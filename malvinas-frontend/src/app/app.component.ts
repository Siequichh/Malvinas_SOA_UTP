import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { LayoutService } from './core/services/layout.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: '<router-outlet />'
})
export class AppComponent implements OnInit {
  constructor(private layout: LayoutService) {}

  ngOnInit(): void {
    this.layout.applyTheme();
  }
}
