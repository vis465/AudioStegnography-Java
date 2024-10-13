from flask import Flask, jsonify, render_template, request, redirect, url_for, send_file, flash
import os
import subprocess
import time

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = 'uploads'
app.config['OUTPUT_FOLDER'] = 'outputs'
app.secret_key = 'your_secret_key'  # Required for flash messages

# Ensure upload and output directories exist
if not os.path.exists(app.config['UPLOAD_FOLDER']):
    os.makedirs(app.config['UPLOAD_FOLDER'])

if not os.path.exists(app.config['OUTPUT_FOLDER']):
    os.makedirs(app.config['OUTPUT_FOLDER'])


# Home page
@app.route('/')
def index():
    return render_template('index.html')

@app.route('/embed', methods=['POST'])
def steganography():
    if 'image' not in request.files or 'audio' not in request.files:
        return jsonify({'error': 'Both image and audio files are required.'}), 400

    image_file = request.files['image']
    audio_file = request.files['audio']

    # Validate that the audio file is a WAV file
    if not audio_file.filename.endswith('.wav'):
        return jsonify({'error': 'Audio file must be a WAV file.'}), 400

    image_path = os.path.join(app.config['UPLOAD_FOLDER'], image_file.filename)
    audio_path = os.path.join(app.config['UPLOAD_FOLDER'], audio_file.filename)
    image_file.save(image_path)
    audio_file.save(audio_path)

    output_stego_image_path = os.path.join(app.config['OUTPUT_FOLDER'], 'steg_image.png')

    # Call the Java process to encode the audio into the image
    result = subprocess.run(
        ['java', '-cp', '.', 'imgsteganography', os.path.abspath(image_path), os.path.abspath(output_stego_image_path), os.path.abspath(audio_path)],
        capture_output=True, text=True
    )

    if os.path.exists(output_stego_image_path):
        return jsonify({'redirect_url': url_for('download_stego_image', filename='steg_image.png')}), 200
    else:
        return jsonify({'error': result.stderr}), 500


# Route to download the stego image
@app.route('/download/<filename>')
def download_stego_image(filename):
    return send_file(os.path.join(app.config['OUTPUT_FOLDER'], filename), as_attachment=True)


@app.route('/steganalysis', methods=['POST'])
def steganalysis():
    if 'file' not in request.files:
        flash('Stego image file is required.', 'error')
        return redirect(url_for('index'))

    stego_image_file = request.files['file']
    print(f"Received file: {stego_image_file.filename}")

    # Check if the file is saved correctly
    stego_image_path = os.path.join(app.config['UPLOAD_FOLDER'], stego_image_file.filename)
    stego_image_file.save(stego_image_path)

    unique_audio_filename = f'extracted_audio_{int(time.time())}.mp3'
    output_audio_path = os.path.join(app.config['OUTPUT_FOLDER'], unique_audio_filename)

    # Debug print: Check if the file is saved correctly
    print(f"Stego image path: {stego_image_path}")
    print(f"Output audio path: {output_audio_path}")

    # Call the Java process to decode the audio from the image
    command = [
        'java',
        '-cp', '.',
        'AudioSteganalysis',  # Java class without .class extension
        os.path.abspath(stego_image_path),
        os.path.abspath(output_audio_path)
    ]

    print(f"Executing command: {' '.join(command)}")  # Debugging the command

    result = subprocess.run(command, capture_output=True, text=True)

    print(f"Java stdout: {result.stdout}")  # To capture standard output
    print(f"Java stderr: {result.stderr}")  # To capture error details

    if os.path.exists(output_audio_path):
        return jsonify({'redirect_url': url_for('download_extracted_audio', filename=unique_audio_filename)}), 200
    else:
        return jsonify({'error': 'Audio extraction failed. Please check the image.'}), 500


# Route to download the extracted audio file
@app.route('/download_audio/<filename>')
def download_extracted_audio(filename):
    file_path = os.path.join(app.config['OUTPUT_FOLDER'], filename)

    # Debug print to check if the file exists and path is correct
    print(f"Attempting to download: {file_path}")

    if os.path.exists(file_path):
        return send_file(file_path, as_attachment=True)
    else:
        flash('Audio file not found.', 'error')
        return redirect(url_for('index'))


if __name__ == '__main__':
    app.run(debug=True)
