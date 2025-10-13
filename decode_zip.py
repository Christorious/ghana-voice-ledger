import base64

with open('ghana_voice_ledger_base64.txt', 'r') as f:
    data = f.read().strip()

with open('GhanaVoiceLedger.zip', 'wb') as f:
    f.write(base64.b64decode(data))

print("ZIP file created successfully: GhanaVoiceLedger.zip")