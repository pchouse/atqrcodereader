# PChouse invoice QR code reader

---

 QR code reader and validator of invoices and other Portuguese tax documents, does not read any other QR codes.

Features:

&rarr; Scanning QR codes from Portuguese tax documents  
&rarr; Validation of QR code data  
&rarr; Issuer tax data provided they exist in the VIES  
&rarr; Buyer's fiscal data as long as it exists in the VIES  
&rarr; Information about the AT certification of the program that issued the document  
&rarr; Invoicing software producers can link the APP to a test environment through Rest API for specific validations with their software  

Software producers who intend to use the validation API must create a Rest server that responds of the JSON type according to the following example and define the URL in the settings tab:

```json
{"status": "OK", 
 "message": "A message of the response",
 "fieldsError": [
    {"field":"A", "value": "The A field error"},
    {"field":"B", "value": "The B field error"} 
 ]
}
```

OK or ERROR in response status does not represent if the fields has errors or not, but if it was possible to proceed with the validation  

[![Google play store]<img src="https://raw.githubusercontent.com/steverichey/google-play-badge-svg/master/img/en_get.svg" with=200 height=100 >](https://play.google.com/store/apps/details?id=pt.pchouse.atqrcodereader)

Google Play and the Google Play logo are trademarks of Google LLC. 

## License

Copyright (C) 2022  Reflexão Sistemas e Estudos Informáticos, Lda

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/agpl-3.0.en.html>.

---

By creating a Rest API to respond to the APP the AGPL license doesn't contaminate your software license, however if you pretend to integrate the APP or parts of the APP in your software and the AGPL license doesn't comply your needs please contact tecnica@pchouse.pt