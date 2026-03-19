program BasicDemo;
var
  a, b: integer;
  arr: array [1..10] of integer;
  flag: boolean;
  text: string;
begin
  // arithmetic and precedence
  a := 10;
  b := a div 2 + 3 * (a - 1);

  { logical expression }
  flag := not (a = b) and true or false;
  arr[1] := b mod 2;
  text := 'result';
  WriteLn(text, ': ', b);
end.
