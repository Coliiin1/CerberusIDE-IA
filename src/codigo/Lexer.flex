package codigo;
import static codigo.Tokens.*;
%%
%class Lexer
%type Tokens
l=[a-zA-Z]+
L=[a-zA-Z_]+
D=[0-9]+
espacio=[ ,\t,\r,\n]+
%{
    public String lexema;
%}
%%
clase |
nuevo | 
este |
publico |
privado |
si |
sino |
segun |
caso |
salir |
para |
mientras |
hacer |
verdadero |
falso |
funcion |
retornar |
imprimir |
entero |
real |
caracter |
cadena |
booleano |
nulo |
vacio {lexema=yytext(); return PALABRA_RESERVADA;}
{espacio} {/*Ignore*/}
"//".* {/*Ignore*/}
"+" {lexema=yytext();return OPERADOR_SUMA;}
"-" {lexema=yytext();return OPERADOR_RESTA;}
"*" {lexema=yytext();return OPERADOR_MULTIPLICAR;}
"/" {lexema=yytext();return OPERADOR_DIVISION;}
"%" {lexema=yytext();return OPERADOR_MODULO;}
{l}({L}|{D})* {lexema=yytext(); return IDENTIFICADOR;}
{D}+ {lexema=yytext(); return NUMERO_ENTERO;}
{D}+(\.{D}+) {lexema=yytext(); return NUMERO_REAL;}
 . {lexema=yytext();return ERROR;}