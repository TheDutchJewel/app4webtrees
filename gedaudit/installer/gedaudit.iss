; gedaudit - Windows-Installer mit Inno Setup, nach dem Muster von wtWin.iss (13.10.2026).
; Gradle baut das Programmverzeichnis (:desktop:createDistributable), dieses Skript packt daraus die Setup-exe:
; Sprachwahl beim Start (Deutsch, Englisch, Franzoesisch, Niederlaendisch, Spanisch, vorbelegt mit der Windows-Sprache),
; Installation pro Benutzer ohne Adminrechte, .ged-Dateien werden mit gedaudit verknuepft (kein URL-Schema, anders als wtWin).
; Aufruf durch Gradle (:desktop:packageInno): ISCC.exe /DVersion=0.4.410 /DQuelle=<app-image> /DZiel=<Ausgabeordner> gedaudit.iss

#ifndef Version
  #error "Version fehlt (/DVersion=x.y.z)"
#endif
#ifndef Quelle
  #error "Quelle fehlt (/DQuelle=<Ordner mit gedaudit.exe, app, runtime>)"
#endif
#ifndef Ziel
  #define Ziel "."
#endif

[Setup]
; NIE aendern: an dieser Kennung erkennt der Installer eine vorhandene Installation und ersetzt sie.
AppId={{7D2A9C4E-3F61-4B8A-9E05-2C7B1D6A8F30}
AppName=gedaudit
AppVersion={#Version}
AppVerName=gedaudit {#Version}
AppPublisher=bgg-home.de
AppPublisherURL=https://gedaudit.bgg-home.de/
AppSupportURL=https://github.com/thobgg/app4webtrees/issues
AppUpdatesURL=https://github.com/thobgg/app4webtrees/releases/latest
VersionInfoVersion={#Version}
VersionInfoProductName=gedaudit
VersionInfoProductVersion={#Version}
VersionInfoCompany=bgg-home.de
VersionInfoDescription=gedaudit - checks GEDCOM files (app4webtrees)
; Signatur (SignPath Foundation) setzt der GitHub-Workflow nach dem Bau auf die fertige exe; Inno Setup selbst signiert nichts.
; Pro Benutzer, ohne Adminrechte - wie der bisherige MSI. {autopf} ist dann %LOCALAPPDATA%\Programs.
PrivilegesRequired=lowest
DefaultDirName={autopf}\gedaudit
DefaultGroupName=gedaudit
DisableProgramGroupPage=yes
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
MinVersion=10.0
OutputDir={#Ziel}
OutputBaseFilename=gedaudit-{#Version}
SetupIconFile=..\icons\app.ico
UninstallDisplayIcon={app}\gedaudit.exe
UninstallDisplayName=gedaudit
Compression=lzma2/max
SolidCompression=yes
WizardStyle=modern
; Sprachwahl immer zeigen (vorbelegt mit der Windows-Sprache), beim Update die zuletzt gewaehlte nehmen.
ShowLanguageDialog=yes
LanguageDetectionMethod=uilanguage
UsePreviousLanguage=yes
; Laeuft gedaudit noch, bittet der Installer, es zu schliessen (Dateien in Benutzung).
CloseApplications=yes
RestartApplications=no
ChangesAssociations=yes

[Languages]
Name: "en"; MessagesFile: "compiler:Default.isl"
Name: "de"; MessagesFile: "compiler:Languages\German.isl"
Name: "fr"; MessagesFile: "compiler:Languages\French.isl"
Name: "nl"; MessagesFile: "compiler:Languages\Dutch.isl"
Name: "es"; MessagesFile: "compiler:Languages\Spanish.isl"


[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

; Beim Update erst die alten Programmdateien weg: Jar-Namen tragen Versionsnummern, sonst sammeln sich Reste an.
[InstallDelete]
Type: filesandordirs; Name: "{app}\app"
Type: filesandordirs; Name: "{app}\runtime"

[Files]
Source: "{#Quelle}\*"; DestDir: "{app}"; Flags: recursesubdirs ignoreversion

[Icons]
Name: "{autoprograms}\gedaudit"; Filename: "{app}\gedaudit.exe"
Name: "{autodesktop}\gedaudit"; Filename: "{app}\gedaudit.exe"; Tasks: desktopicon


; .ged-Dateien mit gedaudit oeffnen (Doppelklick im Explorer)
[Registry]
Root: HKA; Subkey: "Software\Classes\.ged"; ValueType: string; ValueName: ""; ValueData: "gedaudit.ged"; Flags: uninsdeletevalue
Root: HKA; Subkey: "Software\Classes\gedaudit.ged"; ValueType: string; ValueName: ""; ValueData: "GEDCOM file"; Flags: uninsdeletekey
Root: HKA; Subkey: "Software\Classes\gedaudit.ged\DefaultIcon"; ValueType: string; ValueName: ""; ValueData: "{app}\gedaudit.exe,0"
Root: HKA; Subkey: "Software\Classes\gedaudit.ged\shell\open\command"; ValueType: string; ValueName: ""; ValueData: """{app}\gedaudit.exe"" ""%1"""
