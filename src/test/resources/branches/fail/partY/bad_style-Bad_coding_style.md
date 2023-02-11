# Exercice commencé
Votre note est de **0**/20.

## Détail
* Part 1 - Compilation & Tests: 4/4
* Part 2 - CI: 2/2
* Part 3 - Code Coverage: 3.37/4
    * Code coverage: 63.16%, expected: > 80.0% with `mvn verify`

* Part 4 - Site API structure: 0/4
    * Unsuccessful response of POST `/api/inscription`: 404
    * Unsuccessful response of GET `/api/travels?userName=83848586-8788-498a-8b8c-8d8e8f909192`: 404

* Part 5 - Prediction API: 0/2
    * Unsuccessful response of GET `/api/temperature?country=Botswana`: 404

* Part 6 - HTTP client and data coherence (colder): 0/2
    * Skipping due to previous errors

* Part 6 - HTTP client and data coherence (warmer): 0/2
    * Skipping due to previous errors

* Git (proper descriptive messages): 0
    * OK

* Coding style: -13
    * fr.lernejo.prediction.LongClass
      * L.3: Class has 113 lines, exceeding the maximum of 80
      * L.8: Unused local variable: `hereForFun`
      * L.22: Unused local variable: `hereForFun`
      * L.36: Unused local variable: `hereForFun`
      * L.50: Unused local variable: `hereForFun`
      * L.64: Unused local variable: `hereForFun`
      * L.78: Unused local variable: `hereForFun`
      * L.92: Unused local variable: `hereForFun`
      * L.106: Unused local variable: `hereForFun`
    * fr.lernejo.travelsite.LongMethod
      * L.5: Method has 44 lines, exceeding the maximum of 15
      * L.5: Method name should follow lowerCamelCase convention, but `very_long_method` found instead
      * L.6: Unused local variable: `hereForFun`
    * fr.lernejo.travelsite.Pojo
      * L.5: The field `machin` must have modifier `final`



*Analyse effectuée à 1970-01-01T00:00:00Z.*
