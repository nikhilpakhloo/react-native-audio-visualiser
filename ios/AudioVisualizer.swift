import AVFoundation
import Accelerate

class AudioVisualizer: HybridAudioVisualizerSpec {
    private var audioEngine: AVAudioEngine?
    private var inputNode: AVAudioInputNode?
    private var isRecordingFlag = false
    
    // Audio analysis data
    private var currentAmplitude: Double = 0.0
    private var frequencyBands: [Double] = Array(repeating: 0.0, count: 7)
    
    private let amplitudeLock = NSLock()
    private let bandsLock = NSLock()
    
    public var hybridContext = margelo.nitro.HybridContext()
    
    public func startRecording() throws {
        if isRecordingFlag { return }
        
        audioEngine = AVAudioEngine()
        guard let engine = audioEngine else { return }
        
        inputNode = engine.inputNode
        guard let input = inputNode else { return }
        
        let recordingFormat = input.outputFormat(forBus: 0)
        
        input.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { [weak self] buffer, time in
            self?.processAudioBuffer(buffer)
        }
        
        do {
            try engine.start()
            isRecordingFlag = true
        } catch {
            print("Failed to start audio engine: \(error)")
            throw error
        }
    }
    
    public func stopRecording() throws {
        guard let engine = audioEngine, let input = inputNode else { return }
        
        input.removeTap(onBus: 0)
        engine.stop()
        
        isRecordingFlag = false
        audioEngine = nil
        inputNode = nil
        
        amplitudeLock.lock()
        currentAmplitude = 0.0
        amplitudeLock.unlock()
        
        bandsLock.lock()
        frequencyBands = Array(repeating: 0.0, count: 7)
        bandsLock.unlock()
    }
    
    public func isRecording() throws -> Bool {
        return isRecordingFlag
    }
    
    public func getAmplitude() throws -> Double {
        amplitudeLock.lock()
        defer { amplitudeLock.unlock() }
        return currentAmplitude
    }
    
    public func getFrequencyBands() throws -> [Double] {
        bandsLock.lock()
        defer { bandsLock.unlock() }
        return frequencyBands
    }
    
    public func requestPermission(_ promise: Promise) throws {
        AVAudioSession.sharedInstance().requestRecordPermission { granted in
            promise.resolve(granted)
        }
    }
    
    private func processAudioBuffer(_ buffer: AVAudioPCMBuffer) {
        guard let channelData = buffer.floatChannelData else { return }
        let channelDataValue = channelData.pointee
        let frameLength = Int(buffer.frameLength)
        
        // Calculate RMS amplitude
        var sum: Float = 0.0
        vDSP_measqv(channelDataValue, 1, &sum, vDSP_Length(frameLength))
        let rms = sqrt(sum / Float(frameLength))
        
        // Boost quiet sounds, tame loud ones with sqrt
        let normalized = min(Double(rms), 1.0)
        let boostedAmplitude = sqrt(normalized) * 2.0
        
        amplitudeLock.lock()
        currentAmplitude = min(boostedAmplitude, 1.0)
        amplitudeLock.unlock()
        
        // Frequency analysis
        analyzeFrequencies(channelDataValue, frameLength: frameLength)
        
        // Update shared view if available
        AudioVisualizerViewManager.updateAmplitude(CGFloat(currentAmplitude))
        AudioVisualizerViewManager.updateFrequencyBands(frequencyBands.map { CGFloat($0) })
    }
    
    private func analyzeFrequencies(_ data: UnsafePointer<Float>, frameLength: Int) {
        let bandsCount = 7
        let chunkSize = frameLength / bandsCount
        
        bandsLock.lock()
        defer { bandsLock.unlock() }
        
        for i in 0..<bandsCount {
            let start = i * chunkSize
            let count = (i == bandsCount - 1) ? (frameLength - start) : chunkSize
            
            var sum: Float = 0.0
            vDSP_sve(data.advanced(by: start), 1, &sum, vDSP_Length(count))
            
            let rawValue = Double(abs(sum) / Float(count))
            // Apply similar sqrt boosting as amplitude
            frequencyBands[i] = min(sqrt(rawValue) * 2.5, 1.0)
        }
    }
}
