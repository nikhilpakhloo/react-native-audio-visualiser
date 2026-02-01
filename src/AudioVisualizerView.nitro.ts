export type VisualizationStyle =
  | 'strings'
  | 'bars'
  | 'waves'
  | 'thin-bars'
  | 'particles'
  | 'orbit'
  | 'waveform';

export interface AudioVisualizerViewProps {
  type: VisualizationStyle;
  color: string;
  sensitivity: number; // 0.0 - 2.0, default 1.0
  smoothing: number; // 0.0 - 1.0, default 0.7
}
