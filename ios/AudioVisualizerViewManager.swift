import Foundation
import React

@objc(AudioVisualizerViewManager)
class AudioVisualizerViewManager: RCTViewManager {
    
    private static var sharedView: AudioVisualizerNativeView?
    
    override func view() -> UIView! {
        let view = AudioVisualizerNativeView()
        AudioVisualizerViewManager.sharedView = view
        return view
    }
    
    override static func requiresMainQueueSetup() -> Bool {
        return true
    }
    
    @objc func setType(_ view: AudioVisualizerNativeView, type: String) {
        view.setType(type)
    }
    
    @objc func setColor(_ view: AudioVisualizerNativeView, color: String) {
        view.setColor(color)
    }
    
    @objc func setSensitivity(_ view: AudioVisualizerNativeView, sensitivity: NSNumber) {
        view.setSensitivity(CGFloat(truncating: sensitivity))
    }
    
    @objc func setSmoothing(_ view: AudioVisualizerNativeView, smoothing: NSNumber) {
        view.setSmoothing(CGFloat(truncating: smoothing))
    }
    
    static func updateAmplitude(_ amplitude: CGFloat) {
        DispatchQueue.main.async {
            sharedView?.updateAmplitude(amplitude)
        }
    }
    
    static func updateFrequencyBands(_ bands: [CGFloat]) {
        DispatchQueue.main.async {
            sharedView?.updateFrequencyBands(bands)
        }
    }
}
