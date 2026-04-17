package codigo;
import static codigo.Tokens.*;
%%
%class Lexer
%type Tokens
l=[a-zA-Z]+
c=[a-zA-Z ]
L=[a-zA-Z_]+
D=[0-9]+
d=[0-9]
S=[!,#,$,%,&,/,(,),=,?,@,-,\+,\*)]+
s=[!,#,$,%,&,/,(,),=,?,@,-,\+,\*]
espacio=[ ,\t,\r,\n]+
%{
    public String lexema;
%}
%%
clase {lexema=yytext(); return PALABRA_RESERVADA_CLA;}
nuevo {lexema=yytext(); return PALABRA_RESERVADA_NUE;}
este {lexema=yytext(); return PALABRA_RESERVADA_EST;}
publico {lexema=yytext(); return PALABRA_RESERVADA_PUB;}
privado {lexema=yytext(); return PALABRA_RESERVADA_PRI;}
si {lexema=yytext(); return PALABRA_RESERVADA_SI;}
sino {lexema=yytext(); return PALABRA_RESERVADA_SIN;}
segun {lexema=yytext(); return PALABRA_RESERVADA_SEG;}
caso {lexema=yytext(); return PALABRA_RESERVADA_CAS;}
salir {lexema=yytext(); return PALABRA_RESERVADA_SAL;}
para {lexema=yytext(); return PALABRA_RESERVADA_PAR;}
mientras {lexema=yytext(); return PALABRA_RESERVADA_MIE;}
hacer {lexema=yytext(); return PALABRA_RESERVADA_HAC;}
verdadero {lexema=yytext(); return PALABRA_RESERVADA_VER;}
falso {lexema=yytext(); return PALABRA_RESERVADA_FAL;}
funcion {lexema=yytext(); return PALABRA_RESERVADA_FUN;}
retornar {lexema=yytext(); return PALABRA_RESERVADA_RET;}
imprimir {lexema=yytext(); return PALABRA_RESERVADA_IMP;}
entero {lexema=yytext(); return PALABRA_RESERVADA_ENT;}
real {lexema=yytext(); return PALABRA_RESERVADA_REA;}
caracter {lexema=yytext(); return PALABRA_RESERVADA_CAR;}
cadena {lexema=yytext(); return PALABRA_RESERVADA_CAD;}
booleano {lexema=yytext(); return PALABRA_RESERVADA_BOO;}
nulo {lexema=yytext(); return PALABRA_RESERVADA_NUL;}
vacio {lexema=yytext(); return PALABRA_RESERVADA_VAC;}
FX {lexema=yytext(); return FX;}
{espacio} {/*Ignore*/}
"//".* {/*Ignore*/}
";" {lexema=yytext();return PUNTO_COMA;}
"." {lexema=yytext();return PUNTO;}
"{" {lexema=yytext();return LLAVE_ABRE;}
"}" {lexema=yytext();return LLAVE_CIERRA;}
"(" {lexema=yytext();return PARENTESIS_ABRE;}
")" {lexema=yytext();return PARENTESIS_CIERRA;}
"[" {lexema=yytext();return CORCHETE_ABRE;}
"]" {lexema=yytext();return CORCHETE_CIERRA;}
"+" {lexema=yytext();return OPERADOR_SUMA;}
"-" {lexema=yytext();return OPERADOR_RESTA;}
"*" {lexema=yytext();return OPERADOR_MULTIPLICAR;}
"/" {lexema=yytext();return OPERADOR_DIVISION;}
"%" {lexema=yytext();return OPERADOR_MODULO;}
"=" {lexema=yytext();return ASIGNACION;}
"++" {lexema=yytext();return INCREMENTO;}
"--" {lexema=yytext();return DECREMENTO;}
"+=" {lexema=yytext();return INC_VARIABLE;}
"-=" {lexema=yytext();return DEC_VARIABLE;}
"*=" {lexema=yytext();return MUL_VARIABLE;}
"/=" {lexema=yytext();return DIV_VARIABLE;}
"==" {lexema=yytext();return IGUAL;}
"!=" {lexema=yytext();return DIFERENTE;}
"<" {lexema=yytext();return MENOR;}
"<=" {lexema=yytext();return MENOR_IGUAL;}
">" {lexema=yytext();return MAYOR;}
">=" {lexema=yytext();return MAYOR_IGUAL;}
"&" {lexema=yytext();return AND;}
"|" {lexema=yytext();return OR;}
"!" {lexema=yytext();return NEGAR;}
{l}({L}|{D})* {lexema=yytext(); return IDENTIFICADOR;}
\"({D}|{L}|{S}|\ )*\" {lexema=yytext(); return TIPO_CADENA;}
\'({d}|{c}|{s})?\' {lexema=yytext(); return TIPO_CARACTER;}
{D} {lexema=yytext(); return NUMERO_ENTERO;}
{D}(\.{D}) {lexema=yytext(); return NUMERO_REAL;}
 . {lexema=yytext();return ERROR;}