param([string]$OutputDir = "docs/demo")
$ErrorActionPreference = 'Stop'

function Get-RepoPath { param([string]$RelativePath) Join-Path (Get-Location) $RelativePath }
function Escape-XmlValue { param([AllowNull()][object]$Value) if ($null -eq $Value) { '' } else { [System.Security.SecurityElement]::Escape([string]$Value) } }
function Get-SpreadsheetCellType { param([AllowNull()][object]$Value) if ($null -eq $Value) { 'String' } elseif ($Value -is [int] -or $Value -is [long] -or $Value -is [decimal] -or $Value -is [double] -or $Value -is [float]) { 'Number' } else { 'String' } }
function Format-SpreadsheetCellValue {
    param([AllowNull()][object]$Value)
    if ($null -eq $Value) { return '' }
    if ($Value -is [decimal]) { return $Value.ToString([System.Globalization.CultureInfo]::InvariantCulture) }
    if ($Value -is [double] -or $Value -is [float]) { return ([decimal]$Value).ToString([System.Globalization.CultureInfo]::InvariantCulture) }
    return Escape-XmlValue $Value
}
function Add-WorksheetXml {
    param([System.Text.StringBuilder]$Builder,[string]$Name,[string[]]$Columns,[object[]]$Rows)
    [void]$Builder.AppendLine(('  <Worksheet ss:Name="{0}">' -f (Escape-XmlValue $Name)))
    [void]$Builder.AppendLine("    <Table>")
    foreach ($column in $Columns) { [void]$Builder.AppendLine('      <Column ss:AutoFitWidth="1"/>') }
    [void]$Builder.AppendLine("      <Row>")
    foreach ($column in $Columns) { [void]$Builder.AppendLine(('        <Cell ss:StyleID="Header"><Data ss:Type="String">{0}</Data></Cell>' -f (Escape-XmlValue $column))) }
    [void]$Builder.AppendLine("      </Row>")
    foreach ($row in $Rows) {
        [void]$Builder.AppendLine("      <Row>")
        foreach ($column in $Columns) {
            $value = $row.$column
            $type = Get-SpreadsheetCellType $value
            $formattedValue = Format-SpreadsheetCellValue $value
            [void]$Builder.AppendLine(('        <Cell><Data ss:Type="{0}">{1}</Data></Cell>' -f $type, $formattedValue))
        }
        [void]$Builder.AppendLine("      </Row>")
    }
    [void]$Builder.AppendLine("    </Table>")
    [void]$Builder.AppendLine("  </Worksheet>")
}
function Write-SpreadsheetWorkbook {
    param([string]$Path,[object[]]$Worksheets)
    $builder = New-Object System.Text.StringBuilder
    [void]$builder.AppendLine('<?xml version="1.0"?>')
    [void]$builder.AppendLine('<?mso-application progid="Excel.Sheet"?>')
    [void]$builder.AppendLine('<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet" xmlns:html="http://www.w3.org/TR/REC-html40">')
    [void]$builder.AppendLine('  <Styles>')
    [void]$builder.AppendLine('    <Style ss:ID="Default" ss:Name="Normal"><Alignment ss:Vertical="Center" ss:WrapText="1"/><Font ss:FontName="Calibri" ss:Size="11"/></Style>')
    [void]$builder.AppendLine('    <Style ss:ID="Header"><Alignment ss:Horizontal="Center" ss:Vertical="Center" ss:WrapText="1"/><Font ss:FontName="Calibri" ss:Size="11" ss:Bold="1" ss:Color="#FFFFFF"/><Interior ss:Color="#1F4E78" ss:Pattern="Solid"/></Style>')
    [void]$builder.AppendLine('  </Styles>')
    foreach ($worksheet in $Worksheets) { Add-WorksheetXml -Builder $builder -Name $worksheet.Name -Columns $worksheet.Columns -Rows $worksheet.Rows }
    [void]$builder.AppendLine('</Workbook>')
    Set-Content -Path $Path -Value $builder.ToString() -Encoding UTF8
}
function Get-AccountingValue { param([string]$Nature,[decimal]$Debit,[decimal]$Credit) if ($Nature -eq 'CREDITO') { $Credit - $Debit } else { $Debit - $Credit } }
function Get-AccountYearValue { param([hashtable]$Index,[int]$Code,[string]$Name,[int]$Year) $key = "{0}|{1}|{2}" -f $Code, $Name, $Year; if ($Index.ContainsKey($key)) { [decimal]$Index[$key] } else { [decimal]0 } }
function New-ReportLineRow { param([string]$Section,[string]$ReportLine,[int]$AccountCode,[string]$AccountName,[decimal]$Value2024,[decimal]$Value2025,[string]$Usage) [pscustomobject]@{ Seccion=$Section; LineaReporte=$ReportLine; CodigoCuenta=$AccountCode; NombreCuenta=$AccountName; Valor2024=$Value2024; Valor2025=$Value2025; ValorAcumuladoCorte2025=$Value2024 + $Value2025; UsoEnBackend=$Usage } }
function New-IncomeLineRow { param([string]$Category,[string]$ReportLine,[string]$AccountCode,[string]$AccountName,[decimal]$Value2024,[decimal]$Value2025,[string]$Usage) [pscustomobject]@{ Categoria=$Category; LineaReporte=$ReportLine; CodigoCuenta=$AccountCode; NombreCuenta=$AccountName; ValorPeriodo2024=$Value2024; ValorPeriodo2025=$Value2025; UsoEnBackend=$Usage } }

$outputPath = Get-RepoPath $OutputDir
if (-not (Test-Path $outputPath)) { New-Item -ItemType Directory -Path $outputPath -Force | Out-Null }
$mockPath = Get-RepoPath "mock/mock-income-statement-account-info.json"
$mockData = (Get-Content $mockPath -Raw | ConvertFrom-Json).accountInfo
$rawRows = $mockData | ForEach-Object {
    $date = [datetime]::ParseExact($_.date, 'dd-MM-yyyy', $null)
    $debit = [decimal]$_.accountingMovement.debit
    $credit = [decimal]$_.accountingMovement.credit
    $accountingValue = Get-AccountingValue -Nature $_.account.nature -Debit $debit -Credit $credit
    [pscustomobject]@{ Id=[int]$_.id; Fecha=$date.ToString('yyyy-MM-dd'); Anio=[int]$date.Year; EntId=[string]$_.entId; CodigoCuenta=[int]$_.account.code; NombreCuenta=[string]$_.account.name; Naturaleza=[string]$_.account.nature; Debito=$debit; Credito=$credit; ValorContable=$accountingValue; DescripcionMovimiento=[string]$_.accountingMovement.description; ComprobanteNumero=[int]$_.voucher.number; ComprobanteTipo=[string]$_.voucher.type; TerceroId=[string]$_.thirdPartyId; CentroCosto=[string]$_.costCenter.name }
} | Sort-Object Fecha, CodigoCuenta
$summaryRows = $rawRows | Group-Object CodigoCuenta, NombreCuenta, Naturaleza, Anio | ForEach-Object {
    $sample = $_.Group[0]
    [pscustomobject]@{ CodigoCuenta=[int]$sample.CodigoCuenta; NombreCuenta=[string]$sample.NombreCuenta; Naturaleza=[string]$sample.Naturaleza; Anio=[int]$sample.Anio; DebitoTotal=[decimal](($_.Group | Measure-Object Debito -Sum).Sum); CreditoTotal=[decimal](($_.Group | Measure-Object Credito -Sum).Sum); ValorContable=[decimal](($_.Group | Measure-Object ValorContable -Sum).Sum) }
} | Sort-Object CodigoCuenta, Anio, NombreCuenta
$summaryIndex = @{}
foreach ($row in $summaryRows) { $summaryIndex[("{0}|{1}|{2}" -f $row.CodigoCuenta, $row.NombreCuenta, $row.Anio)] = [decimal]$row.ValorContable }
$esfRows = @(
    (New-ReportLineRow "Activo corriente" "Efectivo y equivalente al efectivo" 110505 "Activo corriente - Caja" (Get-AccountYearValue $summaryIndex 110505 "Activo corriente - Caja" 2024) (Get-AccountYearValue $summaryIndex 110505 "Activo corriente - Caja" 2025) "Se clasifica como efectivo y se compara por fecha de corte."),
    (New-ReportLineRow "Activo corriente" "Deudores comerciales y otras cuentas por cobrar" 130505 "Activo corriente - Clientes" (Get-AccountYearValue $summaryIndex 130505 "Activo corriente - Clientes" 2024) (Get-AccountYearValue $summaryIndex 130505 "Activo corriente - Clientes" 2025) "Se clasifica como cuenta por cobrar comercial."),
    (New-ReportLineRow "Activo corriente" "Activos financieros (inversion temporal)" 120505 "Activos financieros inversion temporal" (Get-AccountYearValue $summaryIndex 120505 "Activos financieros inversion temporal" 2024) (Get-AccountYearValue $summaryIndex 120505 "Activos financieros inversion temporal" 2025) "Se identifica por codigo/nombre como inversion temporal."),
    (New-ReportLineRow "Activo corriente" "Activos por impuestos corrientes" 135515 "Activos por impuestos corrientes" (Get-AccountYearValue $summaryIndex 135515 "Activos por impuestos corrientes" 2024) (Get-AccountYearValue $summaryIndex 135515 "Activos por impuestos corrientes" 2025) "Se separa del resto de deudores por su naturaleza tributaria."),
    (New-ReportLineRow "Activo corriente" "Activos biologicos" 146505 "Activos biologicos" (Get-AccountYearValue $summaryIndex 146505 "Activos biologicos" 2024) (Get-AccountYearValue $summaryIndex 146505 "Activos biologicos" 2025) "Se presenta como linea independiente en el ESF."),
    (New-ReportLineRow "Activo corriente" "Activos mantenidos para la venta" 180505 "Activos mantenidos para la venta" (Get-AccountYearValue $summaryIndex 180505 "Activos mantenidos para la venta" 2024) (Get-AccountYearValue $summaryIndex 180505 "Activos mantenidos para la venta" 2025) "Se clasifica como activo disponible para venta."),
    (New-ReportLineRow "Activo no corriente" "Propiedad, planta y equipo" 150405 "Activo no corriente - Propiedad planta y equipo" (Get-AccountYearValue $summaryIndex 150405 "Activo no corriente - Propiedad planta y equipo" 2024) (Get-AccountYearValue $summaryIndex 150405 "Activo no corriente - Propiedad planta y equipo" 2025) "Se clasifica como PPE."),
    (New-ReportLineRow "Activo no corriente" "Activos financieros (inversiones permanentes)" 122505 "Activos financieros inversiones permanentes" (Get-AccountYearValue $summaryIndex 122505 "Activos financieros inversiones permanentes" 2024) (Get-AccountYearValue $summaryIndex 122505 "Activos financieros inversiones permanentes" 2025) "Se clasifica como inversion permanente."),
    (New-ReportLineRow "Activo no corriente" "Propiedades de inversion" 151610 "Propiedades de inversion" (Get-AccountYearValue $summaryIndex 151610 "Propiedades de inversion" 2024) (Get-AccountYearValue $summaryIndex 151610 "Propiedades de inversion" 2025) "Se presenta por separado de PPE."),
    (New-ReportLineRow "Activo no corriente" "Activos intangibles" 160505 "Activos intangibles" (Get-AccountYearValue $summaryIndex 160505 "Activos intangibles" 2024) (Get-AccountYearValue $summaryIndex 160505 "Activos intangibles" 2025) "Se identifica por clase 16 e intangibles."),
    (New-ReportLineRow "Activo no corriente" "Otros activos" 170505 "Otros activos diferidos" (Get-AccountYearValue $summaryIndex 170505 "Otros activos diferidos" 2024) (Get-AccountYearValue $summaryIndex 170505 "Otros activos diferidos" 2025) "Se presenta como otros activos."),
    (New-ReportLineRow "Pasivo" "Pasivo corriente - proveedores" 210505 "Pasivo corriente - Proveedores" (Get-AccountYearValue $summaryIndex 210505 "Pasivo corriente - Proveedores" 2024) (Get-AccountYearValue $summaryIndex 210505 "Pasivo corriente - Proveedores" 2025) "Se clasifica como pasivo corriente."),
    (New-ReportLineRow "Pasivo" "Pasivo no corriente - obligaciones financieras" 220505 "Pasivo no corriente - Obligaciones financieras" (Get-AccountYearValue $summaryIndex 220505 "Pasivo no corriente - Obligaciones financieras" 2024) (Get-AccountYearValue $summaryIndex 220505 "Pasivo no corriente - Obligaciones financieras" 2025) "Se clasifica como pasivo financiero de largo plazo."),
    (New-ReportLineRow "Pasivo" "Acreedores comerciales y otras cuentas por pagar" 220505 "Acreedores comerciales y otras cuentas por pagar" (Get-AccountYearValue $summaryIndex 220505 "Acreedores comerciales y otras cuentas por pagar" 2024) (Get-AccountYearValue $summaryIndex 220505 "Acreedores comerciales y otras cuentas por pagar" 2025) "Comparte codigo con otra cuenta, por eso se diferencia por nombre."),
    (New-ReportLineRow "Pasivo" "Pasivos financieros largo plazo" 230505 "Pasivos financieros largo plazo" (Get-AccountYearValue $summaryIndex 230505 "Pasivos financieros largo plazo" 2024) (Get-AccountYearValue $summaryIndex 230505 "Pasivos financieros largo plazo" 2025) "Se presenta como pasivo financiero no corriente."),
    (New-ReportLineRow "Pasivo" "Pasivos por impuestos corrientes" 240805 "Pasivos por impuestos corrientes" (Get-AccountYearValue $summaryIndex 240805 "Pasivos por impuestos corrientes" 2024) (Get-AccountYearValue $summaryIndex 240805 "Pasivos por impuestos corrientes" 2025) "Se presenta por separado en el pasivo."),
    (New-ReportLineRow "Pasivo" "Provisiones" 260505 "Provision litigios" (Get-AccountYearValue $summaryIndex 260505 "Provision litigios" 2024) (Get-AccountYearValue $summaryIndex 260505 "Provision litigios" 2025) "Se clasifica como provision."),
    (New-ReportLineRow "Pasivo" "Pasivos por impuestos diferidos" 270505 "Pasivos por impuestos diferidos" (Get-AccountYearValue $summaryIndex 270505 "Pasivos por impuestos diferidos" 2024) (Get-AccountYearValue $summaryIndex 270505 "Pasivos por impuestos diferidos" 2025) "Se clasifica como impuesto diferido."),
    (New-ReportLineRow "Patrimonio" "Capital social" 310505 "Patrimonio - Capital social" (Get-AccountYearValue $summaryIndex 310505 "Patrimonio - Capital social" 2024) (Get-AccountYearValue $summaryIndex 310505 "Patrimonio - Capital social" 2025) "Componente positivo del patrimonio."),
    (New-ReportLineRow "Patrimonio" "Acciones propias readquiridas" 320505 "Acciones propias readquiridas" (Get-AccountYearValue $summaryIndex 320505 "Acciones propias readquiridas" 2024) (Get-AccountYearValue $summaryIndex 320505 "Acciones propias readquiridas" 2025) "Componente patrimonial con efecto de disminucion."),
    (New-ReportLineRow "Patrimonio" "Prima de emision" 320510 "Prima de emision" (Get-AccountYearValue $summaryIndex 320510 "Prima de emision" 2024) (Get-AccountYearValue $summaryIndex 320510 "Prima de emision" 2025) "Componente positivo del patrimonio."),
    (New-ReportLineRow "Patrimonio" "Reserva legal" 330505 "Patrimonio - Reserva legal" (Get-AccountYearValue $summaryIndex 330505 "Patrimonio - Reserva legal" 2024) (Get-AccountYearValue $summaryIndex 330505 "Patrimonio - Reserva legal" 2025) "Reserva patrimonial."),
    (New-ReportLineRow "Patrimonio" "Resultados acumulados" 360505 "Patrimonio - Resultados acumulados" (Get-AccountYearValue $summaryIndex 360505 "Patrimonio - Resultados acumulados" 2024) (Get-AccountYearValue $summaryIndex 360505 "Patrimonio - Resultados acumulados" 2025) "Resultados acumulados de periodos anteriores."),
    (New-ReportLineRow "Patrimonio" "Dividendos decretados" 370505 "Patrimonio - Dividendos decretados" (Get-AccountYearValue $summaryIndex 370505 "Patrimonio - Dividendos decretados" 2024) (Get-AccountYearValue $summaryIndex 370505 "Patrimonio - Dividendos decretados" 2025) "Disminucion del patrimonio.")
)

$incomeAccountRows = @(
    (New-IncomeLineRow "Ingresos" "Ingresos ordinarios" "413505" "Ingresos operacionales - Ventas nacionales" (Get-AccountYearValue $summaryIndex 413505 "Ingresos operacionales - Ventas nacionales" 2024) (Get-AccountYearValue $summaryIndex 413505 "Ingresos operacionales - Ventas nacionales" 2025) "Se toma como ingreso operacional principal del periodo."),
    (New-IncomeLineRow "Ingresos" "(-) Devoluciones en ventas" "417505" "Ingresos operacionales - Devoluciones en ventas" ([System.Math]::Abs((Get-AccountYearValue $summaryIndex 417505 "Ingresos operacionales - Devoluciones en ventas" 2024))) ([System.Math]::Abs((Get-AccountYearValue $summaryIndex 417505 "Ingresos operacionales - Devoluciones en ventas" 2025))) "Se descuenta de los ingresos ordinarios."),
    (New-IncomeLineRow "Ingresos" "(+) Otros ingresos" "421505" "Otros ingresos - Recuperaciones" (Get-AccountYearValue $summaryIndex 421505 "Otros ingresos - Recuperaciones" 2024) (Get-AccountYearValue $summaryIndex 421505 "Otros ingresos - Recuperaciones" 2025) "Se adiciona despues de la utilidad bruta."),
    (New-IncomeLineRow "Costos" "(-) Costo de ventas" "613505" "Costo de ventas - Mercancia no fabricada por la empresa" (Get-AccountYearValue $summaryIndex 613505 "Costo de ventas - Mercancia no fabricada por la empresa" 2024) (Get-AccountYearValue $summaryIndex 613505 "Costo de ventas - Mercancia no fabricada por la empresa" 2025) "Se descuenta para obtener utilidad bruta."),
    (New-IncomeLineRow "Gastos" "(-) Gastos de administracion" "510505" "Gastos operacionales de administracion - Sueldos" (Get-AccountYearValue $summaryIndex 510505 "Gastos operacionales de administracion - Sueldos" 2024) (Get-AccountYearValue $summaryIndex 510505 "Gastos operacionales de administracion - Sueldos" 2025) "Se descuenta despues de la utilidad bruta."),
    (New-IncomeLineRow "Gastos" "(-) Gastos depreciacion" "516005" "Gastos de administracion - Depreciacion" (Get-AccountYearValue $summaryIndex 516005 "Gastos de administracion - Depreciacion" 2024) (Get-AccountYearValue $summaryIndex 516005 "Gastos de administracion - Depreciacion" 2025) "Se presenta como linea separada de depreciacion."),
    (New-IncomeLineRow "Gastos" "(-) Gastos de ventas" "520505" "Gastos operacionales de ventas - Comisiones" (Get-AccountYearValue $summaryIndex 520505 "Gastos operacionales de ventas - Comisiones" 2024) (Get-AccountYearValue $summaryIndex 520505 "Gastos operacionales de ventas - Comisiones" 2025) "Se descuenta como gasto operacional de ventas."),
    (New-IncomeLineRow "Gastos" "(-) Gastos financieros" "530505" "Gastos financieros - Intereses" (Get-AccountYearValue $summaryIndex 530505 "Gastos financieros - Intereses" 2024) (Get-AccountYearValue $summaryIndex 530505 "Gastos financieros - Intereses" 2025) "Se descuenta antes de impuestos."),
    (New-IncomeLineRow "Impuestos" "(-) Impuesto de renta" "540505" "Impuesto de renta" (Get-AccountYearValue $summaryIndex 540505 "Impuesto de renta" 2024) (Get-AccountYearValue $summaryIndex 540505 "Impuesto de renta" 2025) "Se descuenta despues de la utilidad antes de impuestos."),
    (New-IncomeLineRow "Impuestos" "(-) Otros impuestos" "541005" "Otros impuestos" (Get-AccountYearValue $summaryIndex 541005 "Otros impuestos" 2024) (Get-AccountYearValue $summaryIndex 541005 "Otros impuestos" 2025) "Se descuenta junto con impuesto de renta.")
)
$ordinary2024 = Get-AccountYearValue $summaryIndex 413505 "Ingresos operacionales - Ventas nacionales" 2024
$ordinary2025 = Get-AccountYearValue $summaryIndex 413505 "Ingresos operacionales - Ventas nacionales" 2025
$returns2024 = [System.Math]::Abs((Get-AccountYearValue $summaryIndex 417505 "Ingresos operacionales - Devoluciones en ventas" 2024))
$returns2025 = [System.Math]::Abs((Get-AccountYearValue $summaryIndex 417505 "Ingresos operacionales - Devoluciones en ventas" 2025))
$otherIncome2024 = Get-AccountYearValue $summaryIndex 421505 "Otros ingresos - Recuperaciones" 2024
$otherIncome2025 = Get-AccountYearValue $summaryIndex 421505 "Otros ingresos - Recuperaciones" 2025
$cost2024 = Get-AccountYearValue $summaryIndex 613505 "Costo de ventas - Mercancia no fabricada por la empresa" 2024
$cost2025 = Get-AccountYearValue $summaryIndex 613505 "Costo de ventas - Mercancia no fabricada por la empresa" 2025
$admin2024 = Get-AccountYearValue $summaryIndex 510505 "Gastos operacionales de administracion - Sueldos" 2024
$admin2025 = Get-AccountYearValue $summaryIndex 510505 "Gastos operacionales de administracion - Sueldos" 2025
$depr2024 = Get-AccountYearValue $summaryIndex 516005 "Gastos de administracion - Depreciacion" 2024
$depr2025 = Get-AccountYearValue $summaryIndex 516005 "Gastos de administracion - Depreciacion" 2025
$sales2024 = Get-AccountYearValue $summaryIndex 520505 "Gastos operacionales de ventas - Comisiones" 2024
$sales2025 = Get-AccountYearValue $summaryIndex 520505 "Gastos operacionales de ventas - Comisiones" 2025
$fin2024 = Get-AccountYearValue $summaryIndex 530505 "Gastos financieros - Intereses" 2024
$fin2025 = Get-AccountYearValue $summaryIndex 530505 "Gastos financieros - Intereses" 2025
$tax2024 = Get-AccountYearValue $summaryIndex 540505 "Impuesto de renta" 2024
$tax2025 = Get-AccountYearValue $summaryIndex 540505 "Impuesto de renta" 2025
$otherTax2024 = Get-AccountYearValue $summaryIndex 541005 "Otros impuestos" 2024
$otherTax2025 = Get-AccountYearValue $summaryIndex 541005 "Otros impuestos" 2025

$netOperating2024 = $ordinary2024 - $returns2024
$netOperating2025 = $ordinary2025 - $returns2025
$gross2024 = $netOperating2024 - $cost2024
$gross2025 = $netOperating2025 - $cost2025
$pbt2024 = $gross2024 + $otherIncome2024 - $admin2024 - $sales2024 - $fin2024 - $depr2024
$pbt2025 = $gross2025 + $otherIncome2025 - $admin2025 - $sales2025 - $fin2025 - $depr2025
$pat2024 = $pbt2024 - $tax2024 - $otherTax2024
$pat2025 = $pbt2025 - $tax2025 - $otherTax2025
$legal2024 = [math]::Round([decimal]($pat2024 * 0.10), 2)
$legal2025 = [math]::Round([decimal]($pat2025 * 0.10), 2)
$statutory2024 = [math]::Round([decimal]($pat2024 * 0.05), 2)
$statutory2025 = [math]::Round([decimal]($pat2025 * 0.05), 2)
$result2024 = $pat2024 - $legal2024 - $statutory2024
$result2025 = $pat2025 - $legal2025 - $statutory2025

$incomeSummaryRows = @(
    [pscustomobject]@{ LineaReporte = 'Ingresos ordinarios'; ValorPeriodo2024 = $ordinary2024; ValorPeriodo2025 = $ordinary2025; Explicacion = 'Ventas del periodo.' },
    [pscustomobject]@{ LineaReporte = '(-) Devoluciones en ventas'; ValorPeriodo2024 = $returns2024; ValorPeriodo2025 = $returns2025; Explicacion = 'Se restan del ingreso operacional.' },
    [pscustomobject]@{ LineaReporte = 'INGRESOS NETOS OPERACIONALES'; ValorPeriodo2024 = $netOperating2024; ValorPeriodo2025 = $netOperating2025; Explicacion = 'Ingresos ordinarios menos devoluciones.' },
    [pscustomobject]@{ LineaReporte = '(-) Costo de ventas'; ValorPeriodo2024 = $cost2024; ValorPeriodo2025 = $cost2025; Explicacion = 'Costo del periodo.' },
    [pscustomobject]@{ LineaReporte = 'UTILIDAD BRUTA ORDINARIA'; ValorPeriodo2024 = $gross2024; ValorPeriodo2025 = $gross2025; Explicacion = 'Ingresos netos menos costo de ventas.' },
    [pscustomobject]@{ LineaReporte = '(+) Otros ingresos'; ValorPeriodo2024 = $otherIncome2024; ValorPeriodo2025 = $otherIncome2025; Explicacion = 'Recuperaciones y otros ingresos del periodo.' },
    [pscustomobject]@{ LineaReporte = '(-) Gastos de administracion'; ValorPeriodo2024 = $admin2024; ValorPeriodo2025 = $admin2025; Explicacion = 'Gasto administrativo del periodo.' },
    [pscustomobject]@{ LineaReporte = '(-) Gastos de ventas'; ValorPeriodo2024 = $sales2024; ValorPeriodo2025 = $sales2025; Explicacion = 'Gasto comercial del periodo.' },
    [pscustomobject]@{ LineaReporte = '(-) Gastos financieros'; ValorPeriodo2024 = $fin2024; ValorPeriodo2025 = $fin2025; Explicacion = 'Intereses y otros cargos financieros.' },
    [pscustomobject]@{ LineaReporte = '(-) Gastos depreciacion'; ValorPeriodo2024 = $depr2024; ValorPeriodo2025 = $depr2025; Explicacion = 'Depreciacion presentada de forma separada.' },
    [pscustomobject]@{ LineaReporte = 'UTILIDAD ANTES DE IMPUESTOS'; ValorPeriodo2024 = $pbt2024; ValorPeriodo2025 = $pbt2025; Explicacion = 'Utilidad bruta mas otros ingresos menos gastos operacionales.' },
    [pscustomobject]@{ LineaReporte = '(-) Impuesto de renta'; ValorPeriodo2024 = $tax2024; ValorPeriodo2025 = $tax2025; Explicacion = 'Impuesto de renta del periodo.' },
    [pscustomobject]@{ LineaReporte = '(-) Otros impuestos'; ValorPeriodo2024 = $otherTax2024; ValorPeriodo2025 = $otherTax2025; Explicacion = 'Tributos adicionales del periodo.' },
    [pscustomobject]@{ LineaReporte = 'UTILIDAD DESPUES DE IMPUESTOS'; ValorPeriodo2024 = $pat2024; ValorPeriodo2025 = $pat2025; Explicacion = 'Utilidad antes de impuestos menos impuestos.' },
    [pscustomobject]@{ LineaReporte = '(-) Reserva legal (10 %)'; ValorPeriodo2024 = $legal2024; ValorPeriodo2025 = $legal2025; Explicacion = 'Reserva calculada al 10 % sobre la utilidad despues de impuestos.' },
    [pscustomobject]@{ LineaReporte = '(-) Reserva estatutaria (5 %)'; ValorPeriodo2024 = $statutory2024; ValorPeriodo2025 = $statutory2025; Explicacion = 'Reserva calculada al 5 % sobre la utilidad despues de impuestos.' },
    [pscustomobject]@{ LineaReporte = 'RESULTADO DEL EJERCICIO'; ValorPeriodo2024 = $result2024; ValorPeriodo2025 = $result2025; Explicacion = 'Resultado final despues de restar reservas.' }
)

$demoRows = @(
    [pscustomobject]@{ Reporte = 'Estado de Situacion Financiera'; FechaActual = '2025-03-29'; FechaAnterior = '2024-03-29'; Explicacion = 'El backend consulta movimientos hasta la fecha de corte seleccionada y arma dos cortes comparativos. Si la fecha elegida es posterior al ultimo movimiento disponible, el reporte se genera con los saldos acumulados hasta ese ultimo registro.'; ClaseContable = 'Clases 1, 2 y 3' },
    [pscustomobject]@{ Reporte = 'Estado de Resultados'; FechaActual = '2025-01-01 a 2025-03-29'; FechaAnterior = '2024-01-01 a 2024-03-29'; Explicacion = 'El backend filtra dos periodos independientes y agrupa cuentas de ingresos, costos, gastos e impuestos.'; ClaseContable = 'Clases 4, 5 y 6' }
)

$metadataRows = @(
    [pscustomobject]@{ Campo = 'Archivo fuente'; Valor = 'mock/mock-income-statement-account-info.json' },
    [pscustomobject]@{ Campo = 'Empresa de prueba'; Valor = 'bf4d475f-5d02-4551-b7f0-49a5c426ac0d' },
    [pscustomobject]@{ Campo = 'Movimientos totales'; Valor = [string]$rawRows.Count },
    [pscustomobject]@{ Campo = 'Cuentas unicas'; Valor = [string](($rawRows | ForEach-Object { "$($_.CodigoCuenta)|$($_.NombreCuenta)" } | Select-Object -Unique).Count) },
    [pscustomobject]@{ Campo = 'Anios cubiertos'; Valor = '2024 y 2025' },
    [pscustomobject]@{ Campo = 'Mock usado por'; Valor = 'Estado de Situacion Financiera y Estado de Resultados' },
    [pscustomobject]@{ Campo = 'Fecha de generacion'; Valor = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss') }
)
$csvPath = Join-Path $outputPath 'mockapi_movimientos_crudos.csv'
$rawRows | Export-Csv -Path $csvPath -NoTypeInformation -Encoding UTF8

$excelXmlPath = Join-Path $outputPath 'mockapi_respaldo_docente.xml'
Write-SpreadsheetWorkbook -Path $excelXmlPath -Worksheets @(
    @{ Name = '00_Metadata'; Columns = @('Campo', 'Valor'); Rows = $metadataRows },
    @{ Name = '01_Raw_MockAPI'; Columns = @('Id', 'Fecha', 'Anio', 'EntId', 'CodigoCuenta', 'NombreCuenta', 'Naturaleza', 'Debito', 'Credito', 'ValorContable', 'DescripcionMovimiento', 'ComprobanteNumero', 'ComprobanteTipo', 'TerceroId', 'CentroCosto'); Rows = $rawRows },
    @{ Name = '02_Resumen_Cuenta_Anio'; Columns = @('CodigoCuenta', 'NombreCuenta', 'Naturaleza', 'Anio', 'DebitoTotal', 'CreditoTotal', 'ValorContable'); Rows = $summaryRows },
    @{ Name = '03_ESF_Cuentas'; Columns = @('Seccion', 'LineaReporte', 'CodigoCuenta', 'NombreCuenta', 'Valor2024', 'Valor2025', 'ValorAcumuladoCorte2025', 'UsoEnBackend'); Rows = $esfRows },
    @{ Name = '04_ER_Cuentas'; Columns = @('Categoria', 'LineaReporte', 'CodigoCuenta', 'NombreCuenta', 'ValorPeriodo2024', 'ValorPeriodo2025', 'UsoEnBackend'); Rows = $incomeAccountRows },
    @{ Name = '05_ER_Resumen'; Columns = @('LineaReporte', 'ValorPeriodo2024', 'ValorPeriodo2025', 'Explicacion'); Rows = $incomeSummaryRows },
    @{ Name = '06_Casos_Demo'; Columns = @('Reporte', 'FechaActual', 'FechaAnterior', 'Explicacion', 'ClaseContable'); Rows = $demoRows }
)

$metadataHtml = $metadataRows | ConvertTo-Html -Fragment
$demoHtml = $demoRows | ConvertTo-Html -Fragment
$incomeHtml = $incomeSummaryRows | Select-Object -First 8 | ConvertTo-Html -Fragment
$esfHtml = $esfRows | Select-Object -First 10 | ConvertTo-Html -Fragment
$htmlPath = Join-Path $outputPath 'explicacion_mockapi_reportes.html'
$htmlContent = @"
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="utf-8" />
  <title>Explicacion tecnica del MockAPI para Estados Financieros</title>
  <style>
    body { font-family: Arial, sans-serif; margin: 32px; color: #1f2937; line-height: 1.45; }
    h1, h2, h3 { color: #0f2a4a; }
    h1 { border-bottom: 3px solid #0f2a4a; padding-bottom: 8px; }
    .note { background: #eef5ff; border-left: 5px solid #1f4e78; padding: 12px 16px; margin: 18px 0; }
    table { border-collapse: collapse; width: 100%; margin: 14px 0 22px; font-size: 12px; }
    th, td { border: 1px solid #d1d5db; padding: 8px; vertical-align: top; }
    th { background: #1f4e78; color: #ffffff; }
    code { background: #f3f4f6; padding: 2px 6px; border-radius: 4px; }
    ul { margin-top: 6px; }
  </style>
</head>
<body>
  <h1>Explicacion tecnica del MockAPI para la sustentacion</h1>
  <p>Este documento resume la forma en que el microservicio <code>financial-statement</code> consume y transforma los datos del MockAPI para generar los reportes de <strong>Estado de Situacion Financiera</strong> y <strong>Estado de Resultados</strong> dentro del ecosistema ContApp.</p>
  <h2>1. Proposito del material de apoyo</h2>
  <ul><li>mostrar los datos exactos del MockAPI;</li><li>explicar como el backend los consume y filtra;</li><li>mostrar como esas cuentas se convierten en lineas de los reportes financieros.</li></ul>
  <h2>2. Caracterizacion del MockAPI</h2>
  $metadataHtml
  <p>Cada movimiento del mock contiene fecha, cuenta contable, naturaleza, valor en debito o credito, tercero, centro de costo y comprobante. El backend los interpreta como movimientos contables, no como un reporte final previamente calculado.</p>
  <h2>3. Como lo usa el backend</h2>
  <div class="note"><strong>Flujo tecnico:</strong> el cliente HTTP <code>AccountInfoClient</code> consulta el MockAPI, transforma el JSON a objetos <code>AccountingEntry</code>, filtra por empresa y fechas, y luego el caso de uso <code>FinancialStatementCommandUC</code> aplica la clasificacion contable segun el tipo de reporte.</div>
  <ul><li><strong>Estado de Situacion Financiera:</strong> utiliza cuentas de clases 1, 2 y 3.</li><li><strong>Estado de Resultados:</strong> utiliza cuentas de clases 4, 5 y 6.</li><li>Despues de generar el reporte, el sistema persiste un snapshot, historial y logs en PostgreSQL.</li></ul>
  <h2>4. Explicacion para Estado de Situacion Financiera</h2>
  <p>Para este reporte el backend trabaja con <strong>fechas de corte</strong>. El sistema toma todos los movimientos contables registrados hasta la fecha seleccionada y construye dos vistas comparativas: un corte anterior y un corte actual. Si la fecha elegida es posterior al ultimo movimiento disponible, el reporte igual se genera con los saldos acumulados hasta ese ultimo registro, siempre que no existan movimientos posteriores.</p>
  $demoHtml
  <p>En el archivo Excel adjunto se incluye una hoja llamada <strong>03_ESF_Cuentas</strong> donde se observa que cada cuenta del mock fue asociada a una linea del Estado de Situacion Financiera.</p>
  $esfHtml
  <h2>5. Explicacion para Estado de Resultados</h2>
  <p>Para este reporte el backend trabaja con <strong>periodos cerrados</strong>. Esto significa que el sistema consulta de forma independiente el periodo actual y el periodo anterior, y con base en ello calcula ingresos, costos, gastos, impuestos y resultado del ejercicio.</p>
  <p>La demostracion recomendada es usar <code>2025-01-01 a 2025-03-29</code> como periodo actual y <code>2024-01-01 a 2024-03-29</code> como periodo anterior.</p>
  $incomeHtml
  <h2>6. Como explicarlo de forma profesional en la exposicion</h2>
  <ul><li>Primero mostrar que el MockAPI no devuelve el estado financiero armado, sino movimientos contables base.</li><li>Luego explicar que el backend traduce esos movimientos a un modelo interno uniforme.</li><li>Despues indicar que la clasificacion depende del tipo de reporte: clases 1, 2 y 3 para ESF; clases 4, 5 y 6 para ER.</li><li>Finalmente, mostrar que el reporte generado se persiste y queda disponible para historial, vista previa, descarga y exportacion.</li></ul>
  <h2>7. Archivos entregados</h2>
  <ul><li><code>mockapi_respaldo_docente.xml</code>: archivo compatible con Excel, con hojas de datos crudos, resumen y mapeos.</li><li><code>mockapi_movimientos_crudos.csv</code>: respaldo universal de los movimientos del mock.</li><li><code>explicacion_mockapi_reportes.pdf</code>: documento listo para sustentar el uso del mock y su transformacion en reportes.</li></ul>
</body>
</html>
"@
Set-Content -Path $htmlPath -Value $htmlContent -Encoding UTF8
Write-Host "Generados:"; Write-Host " - $excelXmlPath"; Write-Host " - $csvPath"; Write-Host " - $htmlPath"



